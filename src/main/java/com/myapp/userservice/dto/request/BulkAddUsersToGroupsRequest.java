package com.myapp.userservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for adding a list of users to multiple existing groups.
 */
public class BulkAddUsersToGroupsRequest {

    @NotEmpty(message = "Group IDs list cannot be empty")
    private List<String> groupIds;

    @NotEmpty(message = "Users list cannot be empty")
    @Valid
    private List<UserInfo> users;

    public BulkAddUsersToGroupsRequest() {
    }

    public List<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    public static class UserInfo {
        private String name;

        @jakarta.validation.constraints.NotBlank(message = "Phone is required")
        private String phone;

        public UserInfo() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
