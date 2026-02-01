package com.myapp.identityservice.domain;

/**
 * Status of a group invitation.
 */
public enum InvitationStatus {
    PENDING,    // Invitation is active, waiting for user to sign up
    ACCEPTED,   // User has signed up and been added to the group
    EXPIRED     // Invitation has expired without being accepted
}
