package com.myapp.userservice.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_group_memberships", indexes = {
    @Index(name = "idx_membership_user_id", columnList = "user_id"),
    @Index(name = "idx_membership_group_id", columnList = "group_id"),
    @Index(name = "idx_membership_user_group", columnList = "user_id, group_id"),
    @Index(name = "idx_membership_created_at", columnList = "created_at")
})
public class UserGroupMembership {

    @Id
    @Column(name = "id", length = 30)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_user"))
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_group"))
    private UserGroup group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20, nullable = false)
    private GroupMembershipAction action;

    @Column(name = "performed_by", length = 30, nullable = false)
    private String performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserGroupMembership() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
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

    public GroupMembershipAction getAction() {
        return action;
    }

    public void setAction(GroupMembershipAction action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
