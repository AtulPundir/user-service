package com.myapp.userservice.exception;

import org.springframework.http.HttpStatus;

public class UsageLimitExceededException extends AppException {

    private final int monthlyLimit;
    private final int utilised;
    private final int requested;
    private final int remaining;

    public UsageLimitExceededException(int monthlyLimit, int utilised, int requested) {
        super("Monthly usage limit exceeded", HttpStatus.TOO_MANY_REQUESTS, "USAGE_LIMIT_EXCEEDED");
        this.monthlyLimit = monthlyLimit;
        this.utilised = utilised;
        this.requested = requested;
        this.remaining = Math.max(0, monthlyLimit - utilised);
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public int getUtilised() {
        return utilised;
    }

    public int getRequested() {
        return requested;
    }

    public int getRemaining() {
        return remaining;
    }
}
