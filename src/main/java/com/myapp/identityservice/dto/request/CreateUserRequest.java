package com.myapp.identityservice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateUserRequest {

    private String id;

    private String authUserId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    private Boolean isVerified;

    @Min(value = -1, message = "Monthly task limit must be at least -1")
    private Integer defaultMonthlyTaskLimit;

    private String subscriptionPlanId;

    public CreateUserRequest() {
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

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Integer getDefaultMonthlyTaskLimit() {
        return defaultMonthlyTaskLimit;
    }

    public void setDefaultMonthlyTaskLimit(Integer defaultMonthlyTaskLimit) {
        this.defaultMonthlyTaskLimit = defaultMonthlyTaskLimit;
    }

    public String getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(String subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    @JsonIgnore
    public String getEffectiveId() {
        return id != null ? id : authUserId;
    }

    @JsonIgnore
    public boolean hasValidId() {
        return id != null || authUserId != null;
    }
}
