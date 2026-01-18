package com.myapp.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public JwtUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails) {
            return (JwtUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    public String getCurrentUserId() {
        JwtUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public boolean isAdmin() {
        JwtUserDetails user = getCurrentUser();
        return user != null && user.isAdmin();
    }
}
