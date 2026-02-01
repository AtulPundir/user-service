package com.myapp.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private String id;
    private String authUserId;
    private String name;
    private String email;
    private String phone;
    private boolean isVerified;
    private UserStatus status;
    private int defaultMonthlyTaskLimit;
    private String subscriptionPlanId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<UsageResponse> monthlyUsage;

    public UserResponse() {
    }

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.authUserId = user.getAuthUserId();
        response.name = user.getName();
        response.email = user.getEmail();
        response.phone = user.getPhone();
        response.isVerified = user.isVerified();
        response.status = user.getStatus();
        response.defaultMonthlyTaskLimit = user.getDefaultMonthlyTaskLimit();
        response.subscriptionPlanId = user.getSubscriptionPlanId();
        response.createdAt = user.getCreatedAt();
        response.updatedAt = user.getUpdatedAt();
        return response;
    }

    public static UserResponse fromEntityWithUsage(User user, List<UsageResponse> usage) {
        UserResponse response = fromEntity(user);
        response.monthlyUsage = usage;
        return response;
    }

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

    public List<UsageResponse> getMonthlyUsage() {
        return monthlyUsage;
    }

    public void setMonthlyUsage(List<UsageResponse> monthlyUsage) {
        this.monthlyUsage = monthlyUsage;
    }
}
