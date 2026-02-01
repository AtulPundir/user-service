package com.myapp.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myapp.identityservice.domain.Invitation;

import java.time.Instant;

/**
 * Response DTO for group invitation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvitationResponse {

    private String id;
    private String groupId;
    private String groupName;
    private String identifier;
    private String identifierType;
    private String inviteeName;
    private String status;
    private String invitedBy;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant resolvedAt;
    private String resolvedUserId;

    public InvitationResponse() {
    }

    public static InvitationResponse fromEntity(Invitation invitation) {
        InvitationResponse response = new InvitationResponse();
        response.id = invitation.getId();
        response.groupId = invitation.getGroupId();
        response.identifier = invitation.getIdentifier();
        response.identifierType = invitation.getIdentifierType().name();
        response.inviteeName = invitation.getInviteeName();
        response.status = invitation.getStatus().name();
        response.invitedBy = invitation.getInvitedBy();
        response.createdAt = invitation.getCreatedAt();
        response.expiresAt = invitation.getExpiresAt();
        response.resolvedAt = invitation.getResolvedAt();
        response.resolvedUserId = invitation.getResolvedUserId();
        return response;
    }

    public static InvitationResponse fromEntityWithGroup(Invitation invitation) {
        InvitationResponse response = fromEntity(invitation);
        if (invitation.getGroup() != null) {
            response.groupName = invitation.getGroup().getName();
        }
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getInviteeName() {
        return inviteeName;
    }

    public void setInviteeName(String inviteeName) {
        this.inviteeName = inviteeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedUserId() {
        return resolvedUserId;
    }

    public void setResolvedUserId(String resolvedUserId) {
        this.resolvedUserId = resolvedUserId;
    }
}
