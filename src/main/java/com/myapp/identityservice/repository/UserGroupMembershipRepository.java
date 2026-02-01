package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.GroupMembershipAction;
import com.myapp.identityservice.domain.UserGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, String> {

    @Query("SELECT m FROM UserGroupMembership m WHERE m.user.id = :userId AND m.group.id = :groupId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<UserGroupMembership> findLatestByUserIdAndGroupId(
            @Param("userId") String userId,
            @Param("groupId") String groupId
    );

    @Query("SELECT m FROM UserGroupMembership m JOIN FETCH m.user WHERE m.group.id = :groupId ORDER BY m.createdAt DESC")
    List<UserGroupMembership> findByGroupIdWithUserOrderByCreatedAtDesc(@Param("groupId") String groupId);

    @Query("SELECT m FROM UserGroupMembership m JOIN FETCH m.user WHERE m.group.id = :groupId AND m.user.id = :userId ORDER BY m.createdAt DESC")
    List<UserGroupMembership> findByGroupIdAndUserIdWithUserOrderByCreatedAtDesc(
            @Param("groupId") String groupId,
            @Param("userId") String userId
    );

    @Query("SELECT m FROM UserGroupMembership m WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<UserGroupMembership> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT m FROM UserGroupMembership m JOIN FETCH m.group WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<UserGroupMembership> findByUserIdWithGroupOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query("SELECT DISTINCT m.user.id FROM UserGroupMembership m WHERE m.group.id = :groupId")
    List<String> findDistinctUserIdsByGroupId(@Param("groupId") String groupId);

    default boolean isUserInGroup(String userId, String groupId) {
        return findLatestByUserIdAndGroupId(userId, groupId)
                .map(m -> m.getAction() == GroupMembershipAction.ADDED)
                .orElse(false);
    }
}
