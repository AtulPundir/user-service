package com.myapp.identityservice.dto.response;

import com.myapp.identityservice.domain.ContactAlias;
import java.time.Instant;

public class ContactAliasResponse {

    private String id;
    private String targetUserId;
    private String aliasName;
    private Instant createdAt;
    private Instant updatedAt;

    public static ContactAliasResponse fromEntity(ContactAlias entity) {
        ContactAliasResponse r = new ContactAliasResponse();
        r.id = entity.getId();
        r.targetUserId = entity.getTargetUserId();
        r.aliasName = entity.getAliasName();
        r.createdAt = entity.getCreatedAt();
        r.updatedAt = entity.getUpdatedAt();
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
