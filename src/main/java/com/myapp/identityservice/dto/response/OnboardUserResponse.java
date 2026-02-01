package com.myapp.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for user onboarding operation.
 * Contains the created/found user and resolved invitations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardUserResponse {

    private UserResponse user;
    private boolean userCreated;
    private int invitationsResolved;
    private List<ResolvedInvitation> resolvedInvitations = new ArrayList<>();

    public OnboardUserResponse() {
    }

    public OnboardUserResponse(UserResponse user, boolean userCreated) {
        this.user = user;
        this.userCreated = userCreated;
    }

    public void addResolvedInvitation(ResolvedInvitation invitation) {
        resolvedInvitations.add(invitation);
        invitationsResolved++;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public boolean isUserCreated() {
        return userCreated;
    }

    public void setUserCreated(boolean userCreated) {
        this.userCreated = userCreated;
    }

    public int getInvitationsResolved() {
        return invitationsResolved;
    }

    public void setInvitationsResolved(int invitationsResolved) {
        this.invitationsResolved = invitationsResolved;
    }

    public List<ResolvedInvitation> getResolvedInvitations() {
        return resolvedInvitations;
    }

    public void setResolvedInvitations(List<ResolvedInvitation> resolvedInvitations) {
        this.resolvedInvitations = resolvedInvitations;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResolvedInvitation {
        private String invitationId;
        private String groupId;
        private String groupName;
        private String invitedBy;
        private boolean membershipCreated;
        private String error;

        public ResolvedInvitation() {
        }

        public static ResolvedInvitation success(String invitationId, String groupId, String groupName, String invitedBy) {
            ResolvedInvitation ri = new ResolvedInvitation();
            ri.invitationId = invitationId;
            ri.groupId = groupId;
            ri.groupName = groupName;
            ri.invitedBy = invitedBy;
            ri.membershipCreated = true;
            return ri;
        }

        public static ResolvedInvitation failure(String invitationId, String groupId, String error) {
            ResolvedInvitation ri = new ResolvedInvitation();
            ri.invitationId = invitationId;
            ri.groupId = groupId;
            ri.membershipCreated = false;
            ri.error = error;
            return ri;
        }

        public String getInvitationId() {
            return invitationId;
        }

        public void setInvitationId(String invitationId) {
            this.invitationId = invitationId;
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

        public String getInvitedBy() {
            return invitedBy;
        }

        public void setInvitedBy(String invitedBy) {
            this.invitedBy = invitedBy;
        }

        public boolean isMembershipCreated() {
            return membershipCreated;
        }

        public void setMembershipCreated(boolean membershipCreated) {
            this.membershipCreated = membershipCreated;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
