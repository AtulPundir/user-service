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
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        return cleaned;
    }
}
