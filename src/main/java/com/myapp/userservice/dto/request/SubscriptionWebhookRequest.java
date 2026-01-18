package com.myapp.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubscriptionWebhookRequest {

    public enum EventType {
        SUBSCRIPTION_ACTIVATED,
        SUBSCRIPTION_UPDATED,
        SUBSCRIPTION_CANCELLED
    }

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotBlank(message = "Auth user ID is required")
    private String authUserId;

    private String planId;

    private Integer monthlyLimit;

    private String effectiveFrom;

    private String effectiveUntil;

    public SubscriptionWebhookRequest() {
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Integer getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Integer monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(String effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }
}
