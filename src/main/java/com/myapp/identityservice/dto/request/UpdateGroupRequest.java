package com.myapp.identityservice.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateGroupRequest {

    @Size(min = 1, message = "Name must not be empty")
    private String name;

    private String description;

    private Boolean isActive;

    public UpdateGroupRequest() {
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
