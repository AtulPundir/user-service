package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.*;
import com.myapp.identityservice.dto.request.OnboardUserRequest;
import com.myapp.identityservice.dto.response.InvitationResponse;
import com.myapp.identityservice.dto.response.OnboardUserResponse;
import com.myapp.identityservice.dto.response.UserResponse;
import com.myapp.identityservice.exception.BadRequestException;
import com.myapp.identityservice.exception.ConflictException;
import com.myapp.identityservice.exception.NotFoundException;
import com.myapp.identityservice.repository.GroupInvitationRepository;
import com.myapp.identityservice.repository.UserGroupMembershipRepository;
import com.myapp.identityservice.repository.UserGroupRepository;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.event.EventPublisher;
import com.myapp.identityservice.util.CuidGenerator;
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
    private final ContextInvitationService contextInvitationService;
    private final EventPublisher eventPublisher;
    private final com.myapp.identityservice.client.WowServiceClient wowServiceClient;

    public InvitationService(GroupInvitationRepository invitationRepository,
                            UserRepository userRepository,
                            UserGroupRepository groupRepository,
                            UserGroupMembershipRepository membershipRepository,
                            CuidGenerator cuidGenerator,
                            ContextInvitationService contextInvitationService,
                            EventPublisher eventPublisher,
                            com.myapp.identityservice.client.WowServiceClient wowServiceClient) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.cuidGenerator = cuidGenerator;
        this.contextInvitationService = contextInvitationService;
        this.eventPublisher = eventPublisher;
        this.wowServiceClient = wowServiceClient;
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
            // User already exists - this is a repeat login, ensure invariants hold
            user = existingUser;
            boolean needsSave = false;

            // Backfill identity fields if missing
            if (user.getIdentityKey() == null) {
                String identityKey = normalizedPhone != null ? normalizedPhone : normalizedEmail;
                user.setIdentityKey(identityKey);
                user.setIdentityType(normalizedPhone != null ? IdentityType.PHONE : IdentityType.EMAIL);
                needsSave = true;
            }

            // Enforce verified invariant: OTP login = identity proven = verified
            if (!user.isVerified()) {
                user.setVerified(true);
                needsSave = true;
            }

            // Backfill phone/email if missing
            if (normalizedPhone != null && user.getPhone() == null) {
                user.setPhone(normalizedPhone);
                needsSave = true;
            }
            if (normalizedEmail != null && user.getEmail() == null) {
                user.setEmail(normalizedEmail);
                needsSave = true;
            }

            if (needsSave) {
                user = userRepository.save(user);
            }
            logger.info("Onboarding: User already exists, invariants enforced: authUserId={}", request.getAuthUserId());
        } else {
            // Check if a placeholder user exists for this phone or email
            String identityKey = normalizedPhone != null ? normalizedPhone : normalizedEmail;
            IdentityType identityType = normalizedPhone != null ? IdentityType.PHONE : IdentityType.EMAIL;
            User placeholderUser = userRepository.findByIdentityKey(identityKey).orElse(null);

            if (placeholderUser != null && placeholderUser.getAuthUserId() == null) {
                // Link placeholder to real auth user
                placeholderUser.setAuthUserId(request.getAuthUserId());
                placeholderUser.setName(request.getName());
                placeholderUser.setEmail(normalizedEmail);
                placeholderUser.setPhone(normalizedPhone);
                placeholderUser.setVerified(request.getIsVerified() != null ? request.getIsVerified() : true);
                placeholderUser.setDefaultMonthlyTaskLimit(50);
                user = userRepository.save(placeholderUser);
                userCreated = false;
                logger.info("Onboarding: Linked placeholder user {} to authUserId={}",
                        user.getId(), request.getAuthUserId());
                // Migrate wow-service records from placeholder id to auth id
                if (!user.getId().equals(request.getAuthUserId())) {
                    migrateUserIdInWowService(user.getId(), request.getAuthUserId());
                }
            } else {
                // Check legacy phone/email columns for placeholder users
                // (placeholder may exist with phone/email set but different identityKey)
                User legacyPlaceholder = null;
                if (normalizedPhone != null) {
                    legacyPlaceholder = userRepository.findByPhone(normalizedPhone).orElse(null);
                }
                if (legacyPlaceholder == null && normalizedEmail != null) {
                    legacyPlaceholder = userRepository.findByEmail(normalizedEmail).orElse(null);
                }

                if (legacyPlaceholder != null && legacyPlaceholder.getAuthUserId() == null) {
                    // Link legacy placeholder to real auth user
                    legacyPlaceholder.setAuthUserId(request.getAuthUserId());
                    legacyPlaceholder.setName(request.getName());
                    legacyPlaceholder.setEmail(normalizedEmail);
                    legacyPlaceholder.setPhone(normalizedPhone);
                    legacyPlaceholder.setIdentityKey(identityKey);
                    legacyPlaceholder.setIdentityType(identityType);
                    legacyPlaceholder.setVerified(request.getIsVerified() != null ? request.getIsVerified() : true);
                    legacyPlaceholder.setDefaultMonthlyTaskLimit(50);
                    user = userRepository.save(legacyPlaceholder);
                    userCreated = false;
                    logger.info("Onboarding: Linked legacy placeholder user {} to authUserId={}",
                            user.getId(), request.getAuthUserId());
                    // Migrate wow-service records from placeholder id to auth id
                    if (!user.getId().equals(request.getAuthUserId())) {
                        migrateUserIdInWowService(user.getId(), request.getAuthUserId());
                    }
                } else if (legacyPlaceholder != null) {
                    // A verified user already exists with this phone/email
                    if (normalizedPhone != null && legacyPlaceholder.getPhone() != null
                            && legacyPlaceholder.getPhone().equals(normalizedPhone)) {
                        throw ConflictException.phoneExists();
                    }
                    throw ConflictException.emailExists();
                } else {
                    // Create new user with authUserId from Auth Service
                    user = new User();
                    user.setId(request.getAuthUserId());
                    user.setAuthUserId(request.getAuthUserId());
                    user.setName(request.getName());
                    user.setEmail(normalizedEmail);
                    user.setPhone(normalizedPhone);
                    user.setIdentityKey(identityKey);
                    user.setIdentityType(identityType);
                    user.setVerified(request.getIsVerified() != null ? request.getIsVerified() : true);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setDefaultMonthlyTaskLimit(50);

                    user = userRepository.save(user);
                    userCreated = true;
                    logger.info("Onboarding: User created: id={}, authUserId={}", user.getId(), request.getAuthUserId());
                }
            }
        }

        OnboardUserResponse response = new OnboardUserResponse(UserResponse.fromEntity(user), userCreated);

        // Resolve pending group invitations for this user's email or phone
        resolveInvitations(user, normalizedEmail, normalizedPhone, response);

        // Auto-accept pending context invitations for this user
        try {
            contextInvitationService.autoAcceptPendingInvitations(user.getId());
        } catch (Exception e) {
            logger.warn("Failed to auto-accept context invitations for user {}: {}", user.getId(), e.getMessage());
        }

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
        if (email == null || email.isBlank()) return null;
        return email.toLowerCase().trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        // Remove all non-digit characters except + at the start
        String cleaned = phone.replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        // If no country code, assume it's already in standard format
        return cleaned;
    }

    /**
     * Migrate userId in wow-service: synchronous call (immediate) + outbox event (reliable fallback).
     * The sync call ensures the user can access their data immediately after login.
     * The outbox event ensures delivery even if wow-service is temporarily down.
     */
    private void migrateUserIdInWowService(String oldUserId, String newUserId) {
        // 1. Outbox event — guaranteed delivery via poller retry
        eventPublisher.publishUserIdMigrated(oldUserId, newUserId);

        // 2. Synchronous call — immediate migration so user can access data right after login
        try {
            wowServiceClient.notifyUserIdMigrated(oldUserId, newUserId,
                    "UID_MIG:" + oldUserId + ":" + newUserId);
            logger.info("Synchronous userId migration delivered: {} -> {}", oldUserId, newUserId);
        } catch (Exception e) {
            // Not fatal — the outbox event will deliver it via poller retry
            logger.warn("Synchronous userId migration failed (outbox will retry): {} -> {}, error={}",
                    oldUserId, newUserId, e.getMessage());
        }
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
