package com.myapp.identityservice.dto.response;

import com.myapp.identityservice.domain.GroupMembershipAction;
import com.myapp.identityservice.domain.UserGroupMembership;

import java.time.Instant;

public class MembershipResponse {

    private String id;
    private String userId;
    private String groupId;
    private GroupMembershipAction action;
    private String performedBy;
    private Instant createdAt;
    private UserSummary user;

    public MembershipResponse() {
    }

    public static MembershipResponse fromEntity(UserGroupMembership membership) {
        MembershipResponse response = new MembershipResponse();
        response.id = membership.getId();
        response.userId = membership.getUser() != null ? membership.getUser().getId() : membership.getUserId();
        response.groupId = membership.getGroup() != null ? membership.getGroup().getId() : membership.getGroupId();
        response.action = membership.getAction();
        response.performedBy = membership.getPerformedBy();
        response.createdAt = membership.getCreatedAt();
        return response;
    }

    public static MembershipResponse fromEntityWithUser(UserGroupMembership membership) {
        MembershipResponse response = fromEntity(membership);
        if (membership.getUser() != null) {
            response.user = UserSummary.fromUser(membership.getUser());
        }
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public GroupMembershipAction getAction() {
        return action;
    }

    public void setAction(GroupMembershipAction action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public static class UserSummary {
        private String id;
        private String name;
        private String email;
        private String status;

        public UserSummary() {
        }

        public static UserSummary fromUser(com.myapp.identityservice.domain.User user) {
            UserSummary summary = new UserSummary();
            summary.id = user.getId();
            summary.name = user.getName();
            summary.email = user.getEmail();
            summary.status = user.getStatus().name();
            return summary;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
