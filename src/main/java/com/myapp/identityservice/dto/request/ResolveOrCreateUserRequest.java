package com.myapp.identityservice.dto.request;

import com.myapp.identityservice.domain.IdentityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResolveOrCreateUserRequest {

    @NotBlank(message = "Identity key is required")
    private String identityKey;

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    private String aliasName;

    public String getIdentityKey() { return identityKey; }
    public void setIdentityKey(String identityKey) { this.identityKey = identityKey; }

    public IdentityType getIdentityType() { return identityType; }
    public void setIdentityType(IdentityType identityType) { this.identityType = identityType; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
}
