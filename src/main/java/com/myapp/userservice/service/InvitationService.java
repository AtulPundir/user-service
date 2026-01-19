package com.myapp.userservice.service;

import com.myapp.userservice.domain.*;
import com.myapp.userservice.dto.request.OnboardUserRequest;
import com.myapp.userservice.dto.response.InvitationResponse;
import com.myapp.userservice.dto.response.OnboardUserResponse;
import com.myapp.userservice.dto.response.UserResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.exception.ConflictException;
import com.myapp.userservice.exception.NotFoundException;
import com.myapp.userservice.repository.GroupInvitationRepository;
import com.myapp.userservice.repository.UserGroupMembershipRepository;
import com.myapp.userservice.repository.UserGroupRepository;
import com.myapp.userservice.repository.UserRepository;
import com.myapp.userservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing group invitations and user onboarding.
 * Implements the Invite-Based Placeholder pattern where non-registered users
 * are represented as invitations rather than shadow users.
 */
@Service
public class InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    private final GroupInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository groupRepository;
    private final UserGroupMembershipRepository membershipRepository;
    private final CuidGenerator cuidGenerator;

    public InvitationService(GroupInvitationRepository invitationRepository,
                            UserRepository userRepository,
                            UserGroupRepository groupRepository,
                            UserGroupMembershipRepository membershipRepository,
                            CuidGenerator cuidGenerator) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.cuidGenerator = cuidGenerator;
    }

    /**
     * Create an invitation for a non-registered user to join a group.
     *
     * @param groupId The group to invite to
     * @param identifier The email or phone of the invitee
     * @param inviteeName Optional name for the invitee
     * @param invitedBy The user ID of the person creating the invitation
     * @return The created invitation
     */
    @Transactional
    public InvitationResponse createInvitation(String groupId, String identifier,
                                               String inviteeName, String invitedBy) {
        // Validate group exists and is active
        UserGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.group(groupId));

        if (!group.isActive()) {
            throw new BadRequestException("Group is not active");
        }

        // Normalize identifier and determine type
        String normalizedIdentifier = normalizeIdentifier(identifier);
        Invitation.IdentifierType identifierType = determineIdentifierType(identifier);

        // Check for existing pending invitation
        if (invitationRepository.existsPendingInvitation(normalizedIdentifier, groupId, Instant.now())) {
            throw ConflictException.invitationAlreadyExists();
        }

        // Create invitation
        Invitation invitation = new Invitation();
        invitation.setId(cuidGenerator.generate());
        invitation.setGroup(group);
        invitation.setIdentifier(normalizedIdentifier);
        invitation.setIdentifierType(identifierType);
        invitation.setInviteeName(inviteeName);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(invitedBy);

        Invitation savedInvitation = invitationRepository.save(invitation);
        logger.info("Invitation created: id={}, groupId={}, identifier={}, invitedBy={}",
                savedInvitation.getId(), groupId, maskIdentifier(normalizedIdentifier), invitedBy);

        InvitationResponse response = InvitationResponse.fromEntity(savedInvitation);
        response.setGroupName(group.getName());
        return response;
    }

    /**
     * Onboard a new user from Auth Service and resolve any pending invitations.
     * This is idempotent - if the user already exists, it will just resolve invitations.
     *
     * @param request The onboarding request from Auth Service
     * @return Response containing user info and resolved invitations
     */
    @Transactional
    public OnboardUserResponse onboardUser(OnboardUserRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhone());

        // Check if user already exists (idempotency)
        User existingUser = userRepository.findByAuthUserId(request.getAuthUserId()).orElse(null);

        boolean userCreated = false;
        User user;

        if (existingUser != null) {
            // User already exists - this is a retry, just use existing user
            user = existingUser;
            logger.info("Onboarding: User already exists, using existing: authUserId={}", request.getAuthUserId());
        } else {
            // Check for conflicts with email/phone
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw ConflictException.emailExists();
            }
            if (userRepository.existsByPhone(normalizedPhone)) {
                throw ConflictException.phoneExists();
            }

            // Create new user with authUserId from Auth Service
            user = new User();
            user.setId(request.getAuthUserId()); // Use authUserId as the user ID for consistency
            user.setAuthUserId(request.getAuthUserId());
            user.setName(request.getName());
            user.setEmail(normalizedEmail);
            user.setPhone(normalizedPhone);
            user.setVerified(request.getIsVerified() != null ? request.getIsVerified() : true);
            user.setStatus(UserStatus.ACTIVE);
            user.setDefaultMonthlyTaskLimit(50);

            user = userRepository.save(user);
            userCreated = true;
            logger.info("Onboarding: User created: id={}, authUserId={}", user.getId(), request.getAuthUserId());
        }

        OnboardUserResponse response = new OnboardUserResponse(UserResponse.fromEntity(user), userCreated);

        // Resolve pending invitations for this user's email or phone
        resolveInvitations(user, normalizedEmail, normalizedPhone, response);

        return response;
    }

    /**
     * Resolve all pending invitations matching the user's email or phone.
     */
    private void resolveInvitations(User user, String email, String phone, OnboardUserResponse response) {
        List<Invitation> pendingInvitations = invitationRepository.findPendingByEmailOrPhoneWithGroup(
                email, phone, Instant.now());

        for (Invitation invitation : pendingInvitations) {
            try {
                resolveInvitation(invitation, user, response);
            } catch (Exception e) {
                logger.warn("Failed to resolve invitation: id={}, error={}", invitation.getId(), e.getMessage());
                response.addResolvedInvitation(OnboardUserResponse.ResolvedInvitation.failure(
                        invitation.getId(), invitation.getGroupId(), e.getMessage()));
            }
        }

        if (!pendingInvitations.isEmpty()) {
            logger.info("Resolved {} invitations for user: userId={}", pendingInvitations.size(), user.getId());
        }
    }

    /**
     * Resolve a single invitation by adding the user to the group.
     */
    private void resolveInvitation(Invitation invitation, User user, OnboardUserResponse response) {
        UserGroup group = invitation.getGroup();

        // Skip if group is no longer active
        if (!group.isActive()) {
            logger.info("Skipping invitation for inactive group: invitationId={}, groupId={}",
                    invitation.getId(), group.getId());
            response.addResolvedInvitation(OnboardUserResponse.ResolvedInvitation.failure(
                    invitation.getId(), group.getId(), "Group is no longer active"));
            invitation.expire();
            invitationRepository.save(invitation);
            return;
        }

        // Check if user is already in the group (idempotency)
        if (membershipRepository.isUserInGroup(user.getId(), group.getId())) {
            logger.info("User already in group, marking invitation as accepted: invitationId={}, userId={}, groupId={}",
                    invitation.getId(), user.getId(), group.getId());
            invitation.accept(user.getId());
            invitationRepository.save(invitation);
            response.addResolvedInvitation(OnboardUserResponse.ResolvedInvitation.success(
                    invitation.getId(), group.getId(), group.getName(), invitation.getInvitedBy()));
            return;
        }

        // Create membership
        UserGroupMembership membership = new UserGroupMembership();
        membership.setId(cuidGenerator.generate());
        membership.setGroup(group);
        membership.setUser(user);
        membership.setAction(GroupMembershipAction.ADDED);
        membership.setPerformedBy(invitation.getInvitedBy()); // Original inviter is the performer

        membershipRepository.save(membership);

        // Mark invitation as accepted
        invitation.accept(user.getId());
        invitationRepository.save(invitation);

        logger.info("Invitation resolved: invitationId={}, userId={}, groupId={}",
                invitation.getId(), user.getId(), group.getId());

        response.addResolvedInvitation(OnboardUserResponse.ResolvedInvitation.success(
                invitation.getId(), group.getId(), group.getName(), invitation.getInvitedBy()));
    }

    /**
     * Get all invitations for a group.
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getGroupInvitations(String groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException.group(groupId);
        }

        return invitationRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending invitations for a group.
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getPendingGroupInvitations(String groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException.group(groupId);
        }

        return invitationRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, InvitationStatus.PENDING)
                .stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cancel a pending invitation.
     */
    @Transactional
    public InvitationResponse cancelInvitation(String invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Can only cancel pending invitations");
        }

        invitation.expire();
        invitationRepository.save(invitation);

        logger.info("Invitation cancelled: id={}", invitationId);
        return InvitationResponse.fromEntity(invitation);
    }

    /**
     * Expire all overdue invitations. This can be called by a scheduled job.
     */
    @Transactional
    public int expireOverdueInvitations() {
        int expired = invitationRepository.expireOverdueInvitations(Instant.now());
        if (expired > 0) {
            logger.info("Expired {} overdue invitations", expired);
        }
        return expired;
    }

    /**
     * Determine if an identifier is an email or phone number.
     */
    private Invitation.IdentifierType determineIdentifierType(String identifier) {
        if (identifier.contains("@")) {
            return Invitation.IdentifierType.EMAIL;
        }
        return Invitation.IdentifierType.PHONE;
    }

    /**
     * Normalize an identifier (lowercase email, standardize phone).
     */
    private String normalizeIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return normalizeEmail(identifier);
        }
        return normalizePhone(identifier);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private String normalizePhone(String phone) {
        // Remove all non-digit characters except + at the start
        String cleaned = phone.replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        // If no country code, assume it's already in standard format
        return cleaned;
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        if (identifier.contains("@")) {
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 2) {
                return identifier.substring(0, 1) + "***" + identifier.substring(atIndex);
            }
            return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
        }
        // Phone
        return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 2);
    }
}
