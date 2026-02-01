package com.myapp.identityservice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public static ConflictException userIdExists() {
        return new ConflictException("User with this ID already exists");
    }

    public static ConflictException emailExists() {
        return new ConflictException("User with this email already exists");
    }

    public static ConflictException phoneExists() {
        return new ConflictException("User with this phone already exists");
    }

    public static ConflictException emailInUse() {
        return new ConflictException("Email already in use");
    }

    public static ConflictException groupNameExists() {
        return new ConflictException("Group with this name already exists at this level");
    }

    public static ConflictException groupNameExistsForUser() {
        return new ConflictException("You have already created a group with this name");
    }

    public static ConflictException userAlreadyInGroup() {
        return new ConflictException("User is already in this group");
    }

    public static ConflictException duplicateGroup(String existingGroupId, String existingGroupName) {
        return new ConflictException(
                String.format("A group with the same name '%s' and identical members already exists (groupId: %s)",
                        existingGroupName, existingGroupId));
    }

    public static ConflictException invitationAlreadyExists() {
        return new ConflictException("An invitation for this identifier already exists for this group");
    }

    public static ConflictException invitationAlreadyAwaitingUserAction() {
        return new ConflictException("An invitation for this user in this context is already awaiting user action");
    }

    public static ConflictException invitationAlreadyPending() {
        return new ConflictException("A pending invitation already exists for this user in this context");
    }
}
