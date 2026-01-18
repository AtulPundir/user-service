package com.myapp.userservice.dto.response;

public class UsageCheckResponse {

    private boolean allowed;
    private int monthlyLimit;
    private int utilised;
    private int remaining;
    private boolean isUnlimited;

    public UsageCheckResponse() {
    }

    public static UsageCheckResponse from(int monthlyLimit, int utilised, int requestedAmount) {
        UsageCheckResponse response = new UsageCheckResponse();
        boolean isUnlimited = monthlyLimit < 0;
        int remaining = isUnlimited ? -1 : Math.max(0, monthlyLimit - utilised);

        response.monthlyLimit = monthlyLimit;
        response.utilised = utilised;
        response.remaining = remaining;
        response.isUnlimited = isUnlimited;
        response.allowed = isUnlimited || remaining >= requestedAmount;

        return response;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
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
