package com.myapp.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myapp.identityservice.domain.UserGroup;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupResponse {

    private String id;
    private String name;
    private String description;
    private String parentGroupId;
    private GroupResponse parentGroup;
    private List<GroupResponse> childGroups;
    private boolean isActive;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MembershipResponse> memberships;
    private List<UserResponse> currentMembers;
    private CountInfo _count;

    public GroupResponse() {
    }

    public static GroupResponse fromEntity(UserGroup group) {
        GroupResponse response = new GroupResponse();
        response.id = group.getId();
        response.name = group.getName();
        response.description = group.getDescription();
        response.parentGroupId = group.getParentGroupId();
        response.isActive = group.isActive();
        response.createdBy = group.getCreatedBy();
        response.createdAt = group.getCreatedAt();
        response.updatedAt = group.getUpdatedAt();
        return response;
    }

    public static GroupResponse fromEntityWithParent(UserGroup group) {
        GroupResponse response = fromEntity(group);
        if (group.getParentGroup() != null) {
            response.parentGroup = fromEntity(group.getParentGroup());
        }
        return response;
    }

    public static GroupResponse fromEntityWithChildCount(UserGroup group, int childCount) {
        GroupResponse response = fromEntityWithParent(group);
        response._count = new CountInfo(childCount);
        return response;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(String parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public GroupResponse getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(GroupResponse parentGroup) {
        this.parentGroup = parentGroup;
    }

    public List<GroupResponse> getChildGroups() {
        return childGroups;
    }

    public void setChildGroups(List<GroupResponse> childGroups) {
        this.childGroups = childGroups;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MembershipResponse> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<MembershipResponse> memberships) {
        this.memberships = memberships;
    }

    public List<UserResponse> getCurrentMembers() {
        return currentMembers;
    }

    public void setCurrentMembers(List<UserResponse> currentMembers) {
        this.currentMembers = currentMembers;
    }

    public CountInfo get_count() {
        return _count;
    }

    public void set_count(CountInfo _count) {
        this._count = _count;
    }

    public static class CountInfo {
        private int childGroups;

        public CountInfo() {
        }

        public CountInfo(int childGroups) {
            this.childGroups = childGroups;
        }

        public int getChildGroups() {
            return childGroups;
        }

        public void setChildGroups(int childGroups) {
            this.childGroups = childGroups;
        }
    }
}
