package com.myapp.identityservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchResolveRequest {

    @NotEmpty(message = "User IDs list is required")
    private List<String> userIds;

    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
}
