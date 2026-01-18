package com.myapp.userservice.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public static NotFoundException user(String id) {
        return new NotFoundException("User not found");
    }

    public static NotFoundException userWithAuthId(String authUserId) {
        return new NotFoundException("User not found with authUserId: " + authUserId);
    }

    public static NotFoundException group(String id) {
        return new NotFoundException("Group not found");
    }

    public static NotFoundException parentGroup() {
        return new NotFoundException("Parent group not found");
    }

    public static NotFoundException usage() {
        return new NotFoundException("Usage record not found");
    }
}
