package com.myapp.identityservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "context_invitations", indexes = {
    @Index(name = "idx_ctx_inv_invited_by", columnList = "invited_by"),
    @Index(name = "idx_ctx_inv_target_user", columnList = "target_user_id"),
    @Index(name = "idx_ctx_inv_context", columnList = "context_type, context_id"),
    @Index(name = "idx_ctx_inv_status", columnList = "status"),
    @Index(name = "idx_ctx_inv_expires_at", columnList = "expires_at")
})
public class ContextInvitation {

    public enum ContextType {
        TRIP, COLLABORATION, AGREEMENT, EXPENSE_GROUP
    }

    public enum InvitationChannel {
        SMS, EMAIL, IN_APP
    }

    public enum ContextInvitationStatus {
        PENDING, PENDING_USER_ACTION, ACCEPTED, REJECTED, EXPIRED, CANCELLED
    }

    @Id
    @Column(length = 30)
    private String id;

    @Column(name = "invited_by", length = 30, nullable = false)
    private String invitedBy;

    @Column(name = "target_user_id", length = 30, nullable = false)
    private String targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 20, nullable = false)
    private ContextType contextType;

    @Column(name = "context_id", length = 50, nullable = false)
    private String contextId;

    @Column(name = "context_role", length = 20)
    private String contextRole = "MEMBER";

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10, nullable = false)
    private InvitationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    private ContextInvitationStatus status = ContextInvitationStatus.PENDING;

    @Column(name = "alias_name", length = 255)
    private String aliasName;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Business methods

    public boolean canBeResolved() {
        return (status == ContextInvitationStatus.PENDING
                || status == ContextInvitationStatus.PENDING_USER_ACTION) && !isExpired();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = ContextInvitationStatus.ACCEPTED;
        this.resolvedAt = Instant.now();
    }

    public void reject() {
        this.status = ContextInvitationStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    public void cancel() {
        this.status = ContextInvitationStatus.CANCELLED;
        this.resolvedAt = Instant.now();
    }

    public void expire() {
        this.status = ContextInvitationStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }

    public void markPendingUserAction() {
        this.status = ContextInvitationStatus.PENDING_USER_ACTION;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public ContextType getContextType() { return contextType; }
    public void setContextType(ContextType contextType) { this.contextType = contextType; }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    public String getContextRole() { return contextRole; }
    public void setContextRole(String contextRole) { this.contextRole = contextRole; }

    public InvitationChannel getChannel() { return channel; }
    public void setChannel(InvitationChannel channel) { this.channel = channel; }

    public ContextInvitationStatus getStatus() { return status; }
    public void setStatus(ContextInvitationStatus status) { this.status = status; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getResolvedAt() { return resolvedAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
