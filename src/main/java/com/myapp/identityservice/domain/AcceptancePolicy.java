package com.myapp.identityservice.domain;

import com.myapp.identityservice.domain.ContextInvitation.ContextType;

public enum AcceptancePolicy {
    AUTO_ACCEPT,
    REQUIRE_USER_CONFIRMATION;

    public static AcceptancePolicy forContextType(ContextType type) {
        return switch (type) {
            case TRIP, COLLABORATION -> AUTO_ACCEPT;
            case EXPENSE_GROUP, AGREEMENT -> REQUIRE_USER_CONFIRMATION;
        };
    }
}
