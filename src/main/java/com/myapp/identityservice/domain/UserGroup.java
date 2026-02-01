package com.myapp.identityservice.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_groups", indexes = {
    @Index(name = "idx_group_parent_group_id", columnList = "parent_group_id"),
    @Index(name = "idx_group_is_active", columnList = "is_active"),
    @Index(name = "idx_group_name", columnList = "name"),
    @Index(name = "idx_group_created_by", columnList = "created_by")
})
public class UserGroup {

    @Id
    @Column(name = "id", length = 30)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id", foreignKey = @ForeignKey(name = "fk_group_parent"))
    private UserGroup parentGroup;

    @Column(name = "parent_group_id", insertable = false, updatable = false)
    private String parentGroupId;

    @OneToMany(mappedBy = "parentGroup")
    private List<UserGroup> childGroups = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<UserGroupMembership> memberships = new ArrayList<>();

    public UserGroup() {
    }

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
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(UserGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public List<UserGroup> getChildGroups() {
        return childGroups;
    }

    public void setChildGroups(List<UserGroup> childGroups) {
        this.childGroups = childGroups;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UserGroupMembership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<UserGroupMembership> memberships) {
        this.memberships = memberships;
    }
}
