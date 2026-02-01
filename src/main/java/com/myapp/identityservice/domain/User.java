package com.myapp.identityservice.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_auth_user_id", columnList = "auth_user_id"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_is_verified", columnList = "is_verified"),
    @Index(name = "idx_subscription_plan_id", columnList = "subscription_plan_id"),
    @Index(name = "idx_identity_key", columnList = "identity_key"),
    @Index(name = "idx_identity_type", columnList = "identity_type")
})
public class User {

    @Id
    @Column(name = "id", length = 30)
    private String id;

    @Column(name = "auth_user_id", length = 30, unique = true)
    private String authUserId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "identity_key", length = 255, unique = true, nullable = false)
    private String identityKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", length = 10, nullable = false)
    private IdentityType identityType;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "default_monthly_task_limit", nullable = false)
    private int defaultMonthlyTaskLimit = 50;

    @Column(name = "subscription_plan_id", length = 100)
    private String subscriptionPlanId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMonthlyUsage> monthlyUsage = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroupMembership> groupMemberships = new ArrayList<>();

    public User() {
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

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    public void setIdentityType(IdentityType identityType) {
        this.identityType = identityType;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public int getDefaultMonthlyTaskLimit() {
        return defaultMonthlyTaskLimit;
    }

    public void setDefaultMonthlyTaskLimit(int defaultMonthlyTaskLimit) {
        this.defaultMonthlyTaskLimit = defaultMonthlyTaskLimit;
    }

    public String getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(String subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
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

    public List<UserMonthlyUsage> getMonthlyUsage() {
        return monthlyUsage;
    }

    public void setMonthlyUsage(List<UserMonthlyUsage> monthlyUsage) {
        this.monthlyUsage = monthlyUsage;
    }

    public List<UserGroupMembership> getGroupMemberships() {
        return groupMemberships;
    }

    public void setGroupMemberships(List<UserGroupMembership> groupMemberships) {
        this.groupMemberships = groupMemberships;
    }
}
