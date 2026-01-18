package com.myapp.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AddUserToGroupRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    public AddUserToGroupRequest() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
