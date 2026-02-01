package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.Invitation;
import com.myapp.identityservice.domain.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<Invitation, String> {

    /**
     * Find all pending invitations for a given identifier (email or phone).
     * Used during user onboarding to resolve invitations.
     */
    @Query("SELECT i FROM Invitation i " +
           "JOIN FETCH i.group g " +
           "WHERE i.identifier = :identifier " +
           "AND i.status = :status " +
           "AND i.expiresAt > :now " +
           "AND g.isActive = true")
    List<Invitation> findPendingByIdentifierWithGroup(
            @Param("identifier") String identifier,
            @Param("status") InvitationStatus status,
            @Param("now") Instant now);

    /**
     * Find all pending invitations for either email or phone.
     * Used during user onboarding when we have both identifiers.
     */
    @Query("SELECT i FROM Invitation i " +
           "JOIN FETCH i.group g " +
           "WHERE (i.identifier = :email OR i.identifier = :phone) " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > :now " +
           "AND g.isActive = true")
    List<Invitation> findPendingByEmailOrPhoneWithGroup(
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("now") Instant now);

    /**
     * Check if a pending invitation already exists for this identifier and group.
     * Used to prevent duplicate invitations.
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Invitation i " +
           "WHERE i.identifier = :identifier " +
           "AND i.groupId = :groupId " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > :now")
    boolean existsPendingInvitation(
            @Param("identifier") String identifier,
            @Param("groupId") String groupId,
            @Param("now") Instant now);

    /**
     * Find existing pending invitation for identifier and group.
     */
    @Query("SELECT i FROM Invitation i " +
           "WHERE i.identifier = :identifier " +
           "AND i.groupId = :groupId " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > :now")
    Optional<Invitation> findPendingInvitation(
            @Param("identifier") String identifier,
            @Param("groupId") String groupId,
            @Param("now") Instant now);

    /**
     * Find all invitations for a group (for admin/audit purposes).
     */
    List<Invitation> findByGroupIdOrderByCreatedAtDesc(String groupId);

    /**
     * Find all invitations for a group with a specific status.
     */
    List<Invitation> findByGroupIdAndStatusOrderByCreatedAtDesc(String groupId, InvitationStatus status);

    /**
     * Bulk expire all invitations that have passed their expiry date.
     * This can be run by a scheduled job.
     */
    @Modifying
    @Query("UPDATE Invitation i SET i.status = 'EXPIRED', i.resolvedAt = :now " +
           "WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    int expireOverdueInvitations(@Param("now") Instant now);

    /**
     * Count pending invitations for a group.
     */
    @Query("SELECT COUNT(i) FROM Invitation i " +
           "WHERE i.groupId = :groupId " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > :now")
    int countPendingInvitations(@Param("groupId") String groupId, @Param("now") Instant now);
}
