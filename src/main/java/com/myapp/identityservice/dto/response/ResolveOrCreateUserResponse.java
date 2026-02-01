package com.myapp.identityservice.dto.response;

public class ResolveOrCreateUserResponse {

    private String userId;
    private boolean isVerified;
    private boolean isNew;

    public ResolveOrCreateUserResponse() {}

    public ResolveOrCreateUserResponse(String userId, boolean isVerified, boolean isNew) {
        this.userId = userId;
        this.isVerified = isVerified;
        this.isNew = isNew;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }
}
