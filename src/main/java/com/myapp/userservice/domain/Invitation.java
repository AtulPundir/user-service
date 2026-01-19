package com.myapp.userservice.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Represents an invitation for a non-registered user to join a group.
 * This is used when User A wants to add User C to a group, but User C hasn't signed up yet.
 * When User C signs up, all their pending invitations are resolved and they are added to the groups.
 */
@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitation_group_id", columnList = "group_id"),
    @Index(name = "idx_invitation_identifier", columnList = "identifier"),
    @Index(name = "idx_invitation_status", columnList = "status"),
    @Index(name = "idx_invitation_identifier_status", columnList = "identifier, status"),
    @Index(name = "idx_invitation_expires_at", columnList = "expires_at")
})
public class Invitation {

    private static final int DEFAULT_EXPIRY_DAYS = 30;

    @Id
    @Column(name = "id", length = 30)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_invitation_group"))
    private UserGroup group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private String groupId;

    /**
     * The identifier (email or phone) used to match the invitation when the user signs up.
     * This is normalized (lowercase for email, standardized format for phone).
     */
    @Column(name = "identifier", length = 255, nullable = false)
    private String identifier;

    /**
     * The type of identifier (EMAIL or PHONE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", length = 10, nullable = false)
    private IdentifierType identifierType;

    /**
     * Optional name provided for the invitee (can be updated when they sign up).
     */
    @Column(name = "invitee_name", length = 255)
    private String inviteeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    /**
     * The user ID of the person who created this invitation.
     */
    @Column(name = "invited_by", length = 30, nullable = false)
    private String invitedBy;

    /**
     * The user ID assigned after signup (set when status changes to ACCEPTED).
     */
    @Column(name = "resolved_user_id", length = 30)
    private String resolvedUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Invitation() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.expiresAt == null) {
            this.expiresAt = now.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
        }
    }

    /**
     * Check if this invitation has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this invitation can be resolved (is pending and not expired).
     */
    public boolean canBeResolved() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    /**
     * Mark this invitation as accepted and link to the user.
     */
    public void accept(String userId) {
        this.status = InvitationStatus.ACCEPTED;
        this.resolvedUserId = userId;
        this.resolvedAt = Instant.now();
    }

    /**
     * Mark this invitation as expired.
     */
    public void expire() {
        this.status = InvitationStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserGroup getGroup() {
        return group;
    }

    public void setGroup(UserGroup group) {
        this.group = group;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(IdentifierType identifierType) {
        this.identifierType = identifierType;
    }

    public String getInviteeName() {
        return inviteeName;
    }

    public void setInviteeName(String inviteeName) {
        this.inviteeName = inviteeName;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getResolvedUserId() {
        return resolvedUserId;
    }

    public void setResolvedUserId(String resolvedUserId) {
        this.resolvedUserId = resolvedUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public enum IdentifierType {
        EMAIL,
        PHONE
    }
}
