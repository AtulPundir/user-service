package com.myapp.identityservice.dto.response;

import java.util.Map;

public class BatchResolveResponse {

    private Map<String, UserDisplayInfo> users;

    public BatchResolveResponse() {}

    public BatchResolveResponse(Map<String, UserDisplayInfo> users) {
        this.users = users;
    }

    public Map<String, UserDisplayInfo> getUsers() { return users; }
    public void setUsers(Map<String, UserDisplayInfo> users) { this.users = users; }
}
