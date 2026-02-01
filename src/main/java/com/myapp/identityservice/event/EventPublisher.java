package com.myapp.identityservice.event;

/**
 * Abstraction for publishing domain events to wow-service.
 * Implementations: KafkaEventPublisher (preferred) and OutboxEventPublisher (HTTP fallback).
 *
 * <h3>Event ID Contract</h3>
 * Every event carries a deterministic {@code eventId} derived from business state,
 * not wall-clock time. This guarantees that retries of the same logical state change
 * produce the same eventId, enabling exactly-once processing in consumers.
 * <ul>
 *   <li>INVITATION_ACCEPTED: {@code "INV_ACC:{invitationId}"}</li>
 *   <li>USER_NAME_UPDATED: {@code "USR_UPD:{userId}:{updatedAtEpochMillis}"}</li>
 *   <li>PENDING_USER_ACTION: {@code "PUA:{invitationId}"}</li>
 * </ul>
 */
public interface EventPublisher {

    void publishInvitationAccepted(String invitationId, String targetUserId,
                                     String contextType, String contextId,
                                     String contextRole, String displayName,
                                     String addedBy);

    /**
     * Publish a user name change event.
     *
     * @param userId          the user whose name changed
     * @param newDisplayName  the new canonical display name
     * @param updatedAtMillis epoch millis of User.updatedAt — provides deterministic eventId
     */
    void publishUserNameUpdated(String userId, String newDisplayName, long updatedAtMillis);

    /**
     * Publish notification that an invitation requires explicit user consent.
     * Consumed by notification-service (or logged until one exists).
     */
    void publishPendingUserAction(String invitationId, String targetUserId,
                                    String contextType, String contextId);
}
