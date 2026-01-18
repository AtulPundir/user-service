package com.myapp.userservice.dto.response;

public class CreateGroupWithUsersResponse {

    private GroupResponse group;
    private BulkAddUsersResponse users;

    public CreateGroupWithUsersResponse() {
    }

    public CreateGroupWithUsersResponse(GroupResponse group, BulkAddUsersResponse users) {
        this.group = group;
        this.users = users;
    }

    public GroupResponse getGroup() {
        return group;
    }

    public void setGroup(GroupResponse group) {
        this.group = group;
    }

    public BulkAddUsersResponse getUsers() {
        return users;
    }

    public void setUsers(BulkAddUsersResponse users) {
        this.users = users;
    }
}
