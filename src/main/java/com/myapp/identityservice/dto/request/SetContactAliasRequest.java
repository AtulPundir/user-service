package com.myapp.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SetContactAliasRequest {

    @NotBlank(message = "Target user ID is required")
    private String targetUserId;

    @NotBlank(message = "Alias name is required")
    private String aliasName;

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
}
