package com.myapp.identityservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "contact_aliases",
    uniqueConstraints = @UniqueConstraint(name = "uk_contact_alias_owner_target",
        columnNames = {"owner_user_id", "target_user_id"}),
    indexes = {
        @Index(name = "idx_contact_aliases_owner", columnList = "owner_user_id"),
        @Index(name = "idx_contact_aliases_target", columnList = "target_user_id")
    })
public class ContactAlias {

    @Id
    @Column(length = 30)
    private String id;

    @Column(name = "owner_user_id", length = 30, nullable = false)
    private String ownerUserId;

    @Column(name = "target_user_id", length = 30, nullable = false)
    private String targetUserId;

    @Column(name = "alias_name", length = 255, nullable = false)
    private String aliasName;

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

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
