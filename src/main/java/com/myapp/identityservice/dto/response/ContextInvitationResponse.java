package com.myapp.identityservice.dto.response;

import com.myapp.identityservice.domain.ContextInvitation;
import java.time.Instant;

public class ContextInvitationResponse {

    private String id;
    private String invitedBy;
    private String targetUserId;
    private String contextType;
    private String contextId;
    private String contextRole;
    private String channel;
    private String status;
    private String aliasName;
    private String message;
    private Instant expiresAt;
    private Instant resolvedAt;
    private Instant createdAt;

    public static ContextInvitationResponse fromEntity(ContextInvitation entity) {
        ContextInvitationResponse r = new ContextInvitationResponse();
        r.id = entity.getId();
        r.invitedBy = entity.getInvitedBy();
        r.targetUserId = entity.getTargetUserId();
        r.contextType = entity.getContextType().name();
        r.contextId = entity.getContextId();
        r.contextRole = entity.getContextRole();
        r.channel = entity.getChannel().name();
        r.status = entity.getStatus().name();
        r.aliasName = entity.getAliasName();
        r.message = entity.getMessage();
        r.expiresAt = entity.getExpiresAt();
        r.resolvedAt = entity.getResolvedAt();
        r.createdAt = entity.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    public String getContextRole() { return contextRole; }
    public void setContextRole(String contextRole) { this.contextRole = contextRole; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
