package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.ContextInvitation;
import com.myapp.identityservice.domain.ContextInvitation.ContextInvitationStatus;
import com.myapp.identityservice.domain.ContextInvitation.ContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ContextInvitationRepository extends JpaRepository<ContextInvitation, String> {

    List<ContextInvitation> findByTargetUserIdAndStatus(String targetUserId, ContextInvitationStatus status);

    List<ContextInvitation> findByContextTypeAndContextId(ContextType contextType, String contextId);

    @Query("SELECT ci FROM ContextInvitation ci WHERE ci.targetUserId = :userId " +
           "AND ci.status = 'PENDING' AND ci.expiresAt > :now ORDER BY ci.createdAt DESC")
    List<ContextInvitation> findPendingByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @Query("SELECT ci FROM ContextInvitation ci WHERE ci.targetUserId = :userId " +
           "AND ci.status IN ('PENDING', 'PENDING_USER_ACTION') AND ci.expiresAt > :now ORDER BY ci.createdAt DESC")
    List<ContextInvitation> findActionableByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @Query("SELECT ci FROM ContextInvitation ci WHERE ci.invitedBy = :userId ORDER BY ci.createdAt DESC")
    List<ContextInvitation> findByInvitedBy(@Param("userId") String userId);

    boolean existsByTargetUserIdAndContextTypeAndContextIdAndStatus(
            String targetUserId, ContextType contextType, String contextId, ContextInvitationStatus status);

    @Modifying
    @Query("UPDATE ContextInvitation ci SET ci.status = 'EXPIRED', ci.resolvedAt = :now, ci.updatedAt = :now " +
           "WHERE ci.status IN ('PENDING', 'PENDING_USER_ACTION') AND ci.expiresAt < :now")
    int expireOverdueInvitations(@Param("now") Instant now);
}
