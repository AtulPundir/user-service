package com.myapp.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk add users to multiple groups operation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkAddUsersToGroupsResponse {

    private int totalGroupsProcessed;
    private int successfulGroups;
    private int failedGroups;
    private List<GroupResult> results = new ArrayList<>();

    public BulkAddUsersToGroupsResponse() {
    }

    public void addGroupResult(GroupResult result) {
        results.add(result);
        totalGroupsProcessed++;
        if (result.isSuccess()) {
            successfulGroups++;
        } else {
            failedGroups++;
        }
    }

    public int getTotalGroupsProcessed() {
        return totalGroupsProcessed;
    }

    public void setTotalGroupsProcessed(int totalGroupsProcessed) {
        this.totalGroupsProcessed = totalGroupsProcessed;
    }

    public int getSuccessfulGroups() {
        return successfulGroups;
    }

    public void setSuccessfulGroups(int successfulGroups) {
        this.successfulGroups = successfulGroups;
    }

    public int getFailedGroups() {
        return failedGroups;
    }

    public void setFailedGroups(int failedGroups) {
        this.failedGroups = failedGroups;
    }

    public List<GroupResult> getResults() {
        return results;
    }

    public void setResults(List<GroupResult> results) {
        this.results = results;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GroupResult {
        private String groupId;
        private String groupName;
        private boolean success;
        private String error;
        private BulkAddUsersResponse usersResult;

        public GroupResult() {
        }

        public static GroupResult success(String groupId, String groupName, BulkAddUsersResponse usersResult) {
            GroupResult result = new GroupResult();
            result.groupId = groupId;
            result.groupName = groupName;
            result.success = true;
            result.usersResult = usersResult;
            return result;
        }

        public static GroupResult failure(String groupId, String error) {
            GroupResult result = new GroupResult();
            result.groupId = groupId;
            result.success = false;
            result.error = error;
            return result;
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

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public BulkAddUsersResponse getUsersResult() {
            return usersResult;
        }

        public void setUsersResult(BulkAddUsersResponse usersResult) {
            this.usersResult = usersResult;
        }
    }
}
