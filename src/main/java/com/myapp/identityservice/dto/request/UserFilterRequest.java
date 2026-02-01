package com.myapp.identityservice.dto.request;

import com.myapp.identityservice.domain.UserStatus;

/**
 * Filter criteria for listing users.
 * Extensible for future filter requirements.
 */
public class UserFilterRequest {

    private UserStatus status;
    private Boolean verified;
    private String search;  // Future: search by name, email, or phone

    public UserFilterRequest() {
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public boolean hasFilters() {
        return status != null || verified != null || search != null;
    }
}
