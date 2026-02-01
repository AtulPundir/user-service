package com.myapp.identityservice.dto.request;

import com.myapp.identityservice.domain.ContextInvitation.ContextType;
import com.myapp.identityservice.domain.ContextInvitation.InvitationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateContextInvitationRequest {

    @NotBlank(message = "Target user ID is required")
    private String targetUserId;

    @NotNull(message = "Context type is required")
    private ContextType contextType;

    @NotBlank(message = "Context ID is required")
    private String contextId;

    private String contextRole;

    @NotNull(message = "Channel is required")
    private InvitationChannel channel;

    private String aliasName;
    private String message;

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public ContextType getContextType() { return contextType; }
    public void setContextType(ContextType contextType) { this.contextType = contextType; }

    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    public String getContextRole() { return contextRole; }
    public void setContextRole(String contextRole) { this.contextRole = contextRole; }

    public InvitationChannel getChannel() { return channel; }
    public void setChannel(InvitationChannel channel) { this.channel = channel; }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
