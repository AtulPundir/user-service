package com.myapp.identityservice.dto.response;

import com.myapp.identityservice.domain.UserMonthlyUsage;

public class UsageResponse {

    private String id;
    private String userId;
    private int year;
    private int month;
    private int monthlyLimit;
    private int utilised;
    private int remaining;
    private boolean isUnlimited;

    public UsageResponse() {
    }

    public static UsageResponse fromEntity(UserMonthlyUsage usage) {
        UsageResponse response = new UsageResponse();
        response.id = usage.getId();
        response.userId = usage.getUser() != null ? usage.getUser().getId() : usage.getUserId();
        response.year = usage.getYear();
        response.month = usage.getMonth();
        response.monthlyLimit = usage.getMonthlyLimit();
        response.utilised = usage.getUtilised();
        response.isUnlimited = usage.isUnlimited();
        response.remaining = usage.getRemaining();
        return response;
    }

    public static UsageResponse defaultUsage(String userId, int year, int month, int monthlyLimit) {
        UsageResponse response = new UsageResponse();
        response.userId = userId;
        response.year = year;
        response.month = month;
        response.monthlyLimit = monthlyLimit;
        response.utilised = 0;
        response.isUnlimited = monthlyLimit < 0;
        response.remaining = monthlyLimit < 0 ? -1 : monthlyLimit;
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(int monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getUtilised() {
        return utilised;
    }

    public void setUtilised(int utilised) {
        this.utilised = utilised;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public boolean isUnlimited() {
        return isUnlimited;
    }

    public void setUnlimited(boolean unlimited) {
        isUnlimited = unlimited;
    }
}
