package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.ContextInvitation;
import com.myapp.identityservice.event.EventPublisher;
import com.myapp.identityservice.domain.ContextInvitation.ContextInvitationStatus;
import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.dto.request.CreateContextInvitationRequest;
import com.myapp.identityservice.dto.response.ContextInvitationResponse;
import com.myapp.identityservice.exception.BadRequestException;
import com.myapp.identityservice.exception.ConflictException;
import com.myapp.identityservice.exception.NotFoundException;
import com.myapp.identityservice.repository.ContextInvitationRepository;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myapp.identityservice.domain.AcceptancePolicy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContextInvitationService {

    private static final Logger logger = LoggerFactory.getLogger(ContextInvitationService.class);

    private final ContextInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CuidGenerator cuidGenerator;
    private final EventPublisher eventPublisher;
    private final com.myapp.identityservice.client.NotificationServiceClient notificationServiceClient;

    @Value("${app.invitation.expiry-days:30}")
    private int expiryDays;

    public ContextInvitationService(ContextInvitationRepository invitationRepository,
                                     UserRepository userRepository,
                                     CuidGenerator cuidGenerator,
                                     EventPublisher eventPublisher,
                                     com.myapp.identityservice.client.NotificationServiceClient notificationServiceClient) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.cuidGenerator = cuidGenerator;
        this.eventPublisher = eventPublisher;
        this.notificationServiceClient = notificationServiceClient;
    }

    @Transactional
    public ContextInvitationResponse createInvitation(String invitedByUserId,
                                                       CreateContextInvitationRequest request) {
        // Validate target user exists
        userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new NotFoundException("Target user not found: " + request.getTargetUserId()));

        // Check for duplicate pending or awaiting-action invitation
        if (invitationRepository.existsByTargetUserIdAndContextTypeAndContextIdAndStatus(
                request.getTargetUserId(), request.getContextType(),
                request.getContextId(), ContextInvitationStatus.PENDING)) {
            throw ConflictException.invitationAlreadyPending();
        }
        if (invitationRepository.existsByTargetUserIdAndContextTypeAndContextIdAndStatus(
                request.getTargetUserId(), request.getContextType(),
                request.getContextId(), ContextInvitationStatus.PENDING_USER_ACTION)) {
            throw ConflictException.invitationAlreadyAwaitingUserAction();
        }

        ContextInvitation invitation = new ContextInvitation();
        invitation.setId(cuidGenerator.generate());
        invitation.setInvitedBy(invitedByUserId);
        invitation.setTargetUserId(request.getTargetUserId());
        invitation.setContextType(request.getContextType());
        invitation.setContextId(request.getContextId());
        invitation.setContextRole(request.getContextRole() != null ? request.getContextRole() : "MEMBER");
        invitation.setChannel(request.getChannel());
        invitation.setStatus(ContextInvitationStatus.PENDING);
        invitation.setAliasName(request.getAliasName());
        invitation.setMessage(request.getMessage());
        invitation.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));

        ContextInvitation saved = invitationRepository.save(invitation);
        logger.info("Created context invitation: id={}, contextType={}, contextId={}, target={}",
                saved.getId(), saved.getContextType(), saved.getContextId(), saved.getTargetUserId());

        // Send notification to invitee
        sendInvitationCreatedNotification(saved, invitedByUserId, request.getTargetUserId());

        return ContextInvitationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ContextInvitationResponse> getMyPendingInvitations(String userId) {
        return invitationRepository.findActionableByUserId(userId, Instant.now())
                .stream()
                .map(ContextInvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContextInvitationResponse acceptInvitation(String invitationId, String userId) {
        ContextInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));

        if (!invitation.getTargetUserId().equals(userId)) {
            throw new BadRequestException("You are not the target of this invitation");
        }

        if (!invitation.canBeResolved()) {
            if (invitation.isExpired()) {
                throw new BadRequestException("This invitation has expired");
            }
            throw new BadRequestException("This invitation has already been resolved");
        }

        invitation.accept();
        ContextInvitation saved = invitationRepository.save(invitation);

        logger.info("Context invitation accepted: id={}, userId={}", invitationId, userId);

        // Notify wow-service asynchronously
        notifyWowServiceOfAcceptance(saved, userId);

        // Send notification to inviter that invitation was accepted
        sendInvitationAcceptedNotification(saved, userId);

        return ContextInvitationResponse.fromEntity(saved);
    }

    @Transactional
    public ContextInvitationResponse rejectInvitation(String invitationId, String userId) {
        ContextInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));

        if (!invitation.getTargetUserId().equals(userId)) {
            throw new BadRequestException("You are not the target of this invitation");
        }

        if (!invitation.canBeResolved()) {
            if (invitation.isExpired()) {
                throw new BadRequestException("This invitation has expired");
            }
            throw new BadRequestException("This invitation has already been resolved");
        }

        invitation.reject();
        ContextInvitation saved = invitationRepository.save(invitation);

        logger.info("Context invitation rejected: id={}, userId={}", invitationId, userId);
        return ContextInvitationResponse.fromEntity(saved);
    }

    @Transactional
    public int expireOverdueInvitations() {
        int expired = invitationRepository.expireOverdueInvitations(Instant.now());
        if (expired > 0) {
            logger.info("Expired {} overdue context invitations", expired);
        }
        return expired;
    }

    /**
     * Process pending invitations when a user verifies.
     * AUTO_ACCEPT contexts (TRIP, COLLABORATION) are accepted immediately.
     * REQUIRE_USER_CONFIRMATION contexts (EXPENSE_GROUP, AGREEMENT) transition to
     * PENDING_USER_ACTION, requiring explicit user consent via the accept API.
     *
     * Returns only the auto-accepted invitations.
     */
    @Transactional
    public List<ContextInvitation> autoAcceptPendingInvitations(String userId) {
        List<ContextInvitation> pending = invitationRepository.findPendingByUserId(userId, Instant.now());
        List<ContextInvitation> accepted = new ArrayList<>();

        for (ContextInvitation invitation : pending) {
            AcceptancePolicy policy = AcceptancePolicy.forContextType(invitation.getContextType());

            if (policy == AcceptancePolicy.AUTO_ACCEPT) {
                invitation.accept();
                invitationRepository.save(invitation);
                logger.info("Auto-accepted context invitation: id={}, contextType={}, contextId={}",
                        invitation.getId(), invitation.getContextType(), invitation.getContextId());
                notifyWowServiceOfAcceptance(invitation, userId);
                accepted.add(invitation);
            } else {
                invitation.markPendingUserAction();
                invitationRepository.save(invitation);
                logger.info("Invitation requires user confirmation: id={}, contextType={}, contextId={}",
                        invitation.getId(), invitation.getContextType(), invitation.getContextId());

                // Emit notification event — idempotent per invitation (eventId = "PUA:{invitationId}")
                try {
                    eventPublisher.publishPendingUserAction(
                            invitation.getId(), userId,
                            invitation.getContextType().name(), invitation.getContextId());
                } catch (Exception e) {
                    logger.warn("Failed to emit PENDING_USER_ACTION event: id={}, error={}",
                            invitation.getId(), e.getMessage());
                }
            }
        }

        return accepted;
    }

    private void notifyWowServiceOfAcceptance(ContextInvitation invitation, String userId) {
        try {
            User targetUser = userRepository.findById(userId).orElse(null);
            String displayName = targetUser != null ? targetUser.getName() : null;
            if (displayName == null && invitation.getAliasName() != null) {
                displayName = invitation.getAliasName();
            }

            eventPublisher.publishInvitationAccepted(
                    invitation.getId(),
                    userId,
                    invitation.getContextType().name(),
                    invitation.getContextId(),
                    invitation.getContextRole(),
                    displayName,
                    invitation.getInvitedBy()
            );
        } catch (Exception e) {
            logger.warn("Failed to notify wow-service of invitation acceptance: id={}, error={}",
                    invitation.getId(), e.getMessage());
        }
    }

    /**
     * Send notification to invitee when invitation is created.
     */
    private void sendInvitationCreatedNotification(ContextInvitation invitation, String inviterId, String targetUserId) {
        try {
            // Get inviter details
            User inviter = userRepository.findById(inviterId).orElse(null);
            String inviterName = inviter != null ? inviter.getName() : "Someone";

            // Get target user details
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) {
                logger.warn("Cannot send invitation notification: target user not found: {}", targetUserId);
                return;
            }

            String contextName = invitation.getAliasName(); // Context name often stored as alias
            String message = invitation.getMessage();

            notificationServiceClient.sendInvitationCreated(
                    targetUser.getPhone(),
                    targetUser.getEmail(),
                    inviterName,
                    invitation.getContextType().name(),
                    contextName,
                    message
            );
        } catch (Exception e) {
            logger.warn("Failed to send invitation created notification: invitationId={}, error={}",
                    invitation.getId(), e.getMessage());
        }
    }

    /**
     * Send notification to inviter when invitation is accepted.
     */
    private void sendInvitationAcceptedNotification(ContextInvitation invitation, String acceptingUserId) {
        try {
            // Get inviter details
            User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
            if (inviter == null) {
                logger.warn("Cannot send acceptance notification: inviter not found: {}", invitation.getInvitedBy());
                return;
            }

            // Get accepting user details
            User acceptingUser = userRepository.findById(acceptingUserId).orElse(null);
            String inviteeName = acceptingUser != null ? acceptingUser.getName() : invitation.getAliasName();
            if (inviteeName == null) inviteeName = "Someone";

            String contextName = invitation.getAliasName();

            notificationServiceClient.sendInvitationAccepted(
                    inviter.getPhone(),
                    inviter.getEmail(),
                    inviteeName,
                    invitation.getContextType().name(),
                    contextName
            );
        } catch (Exception e) {
            logger.warn("Failed to send invitation accepted notification: invitationId={}, error={}",
                    invitation.getId(), e.getMessage());
        }
    }
}
