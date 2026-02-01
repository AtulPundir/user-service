package com.myapp.identityservice.security;

public class JwtUserDetails {

    private String userId;
    private String phone;
    private String role;

    public JwtUserDetails() {
    }

    public JwtUserDetails(String userId, String phone, String role) {
        this.userId = userId;
        this.phone = phone;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
