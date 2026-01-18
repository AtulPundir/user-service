package com.myapp.userservice.dto.response;

public class UsageConsumeResponse {

    private boolean success;
    private int monthlyLimit;
    private int utilised;
    private int remaining;
    private boolean isUnlimited;

    public UsageConsumeResponse() {
    }

    public static UsageConsumeResponse from(int monthlyLimit, int utilised) {
        UsageConsumeResponse response = new UsageConsumeResponse();
        boolean isUnlimited = monthlyLimit < 0;
        int remaining = isUnlimited ? -1 : Math.max(0, monthlyLimit - utilised);

        response.success = true;
        response.monthlyLimit = monthlyLimit;
        response.utilised = utilised;
        response.remaining = remaining;
        response.isUnlimited = isUnlimited;

        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
