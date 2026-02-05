package com.myapp.identityservice.util;

import com.myapp.identityservice.domain.IdentityType;
import org.springframework.stereotype.Component;

@Component
public class IdentifierNormalizer {

    public String normalize(String identifier, IdentityType type) {
        if (type == IdentityType.EMAIL) {
            return normalizeEmail(identifier);
        }
        return normalizePhone(identifier);
    }

    public IdentityType detectType(String identifier) {
        if (identifier != null && identifier.contains("@")) {
            return IdentityType.EMAIL;
        }
        return IdentityType.PHONE;
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private String normalizePhone(String phone) {
        // Strip everything except digits and leading +
        String cleaned = phone.replaceAll("[^\\d+]", "");

        // Phone must include country code (start with +)
        if (!cleaned.startsWith("+")) {
            throw new IllegalArgumentException(
                "Phone number must include country code in E.164 format (e.g., +919876543210)");
        }

        // Validate minimum length: + plus at least 9 digits
        if (cleaned.length() < 10) {
            throw new IllegalArgumentException(
                "Phone number is too short. Must be in E.164 format with country code (e.g., +919876543210)");
        }

        return cleaned;
    }
}
