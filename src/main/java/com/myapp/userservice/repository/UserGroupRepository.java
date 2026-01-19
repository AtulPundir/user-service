package com.myapp.userservice.repository;

import com.myapp.userservice.domain.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, String> {

    @Query("SELECT g FROM UserGroup g WHERE g.name = :name AND " +
           "((:parentGroupId IS NULL AND g.parentGroup IS NULL) OR g.parentGroup.id = :parentGroupId)")
    Optional<UserGroup> findByNameAndParentGroupId(
            @Param("name") String name,
            @Param("parentGroupId") String parentGroupId
    );

    @Query("SELECT g FROM UserGroup g WHERE g.isActive = :isActive ORDER BY g.createdAt DESC")
    Page<UserGroup> findByIsActiveOrderByCreatedAtDesc(@Param("isActive") boolean isActive, Pageable pageable);

    @Query("SELECT g FROM UserGroup g WHERE g.parentGroup.id = :parentGroupId ORDER BY g.createdAt DESC")
    Page<UserGroup> findByParentGroupIdOrderByCreatedAtDesc(@Param("parentGroupId") String parentGroupId, Pageable pageable);

    @Query("SELECT g FROM UserGroup g WHERE g.isActive = :isActive AND g.parentGroup.id = :parentGroupId ORDER BY g.createdAt DESC")
    Page<UserGroup> findByIsActiveAndParentGroupIdOrderByCreatedAtDesc(
            @Param("isActive") boolean isActive,
            @Param("parentGroupId") String parentGroupId,
            Pageable pageable
    );

    @Query("SELECT g FROM UserGroup g ORDER BY g.createdAt DESC")
    Page<UserGroup> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT g FROM UserGroup g LEFT JOIN FETCH g.parentGroup WHERE g.id = :id")
    Optional<UserGroup> findByIdWithParent(@Param("id") String id);

    @Query("SELECT g FROM UserGroup g LEFT JOIN FETCH g.parentGroup LEFT JOIN FETCH g.childGroups WHERE g.id = :id")
    Optional<UserGroup> findByIdWithParentAndChildren(@Param("id") String id);

    @Query("SELECT COUNT(g) FROM UserGroup g WHERE g.parentGroup.id = :parentGroupId AND g.isActive = true")
    int countActiveChildGroups(@Param("parentGroupId") String parentGroupId);

    @Query("SELECT COUNT(g) FROM UserGroup g WHERE g.parentGroup.id = :parentGroupId")
    int countChildGroups(@Param("parentGroupId") String parentGroupId);

    List<UserGroup> findByParentGroupIdAndIsActiveTrue(String parentGroupId);

    boolean existsByNameAndParentGroupId(String name, String parentGroupId);

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM UserGroup g WHERE g.name = :name AND g.parentGroup IS NULL")
    boolean existsByNameAndParentGroupIsNull(@Param("name") String name);

    /**
     * Find all active groups with the same name (case insensitive), excluding a specific group.
     * Used for duplicate detection when adding members to an existing group.
     */
    @Query("SELECT g FROM UserGroup g WHERE LOWER(g.name) = LOWER(:name) AND g.isActive = true AND g.id <> :excludeGroupId")
    List<UserGroup> findByNameIgnoreCaseAndIsActiveTrueAndIdNot(
            @Param("name") String name,
            @Param("excludeGroupId") String excludeGroupId);

    /**
     * Find all active groups with the same name (case insensitive).
     * Used for duplicate detection during group creation.
     */
    @Query("SELECT g FROM UserGroup g WHERE LOWER(g.name) = LOWER(:name) AND g.isActive = true")
    List<UserGroup> findByNameIgnoreCaseAndIsActiveTrue(@Param("name") String name);

    /**
     * Check if an active group with the same name (case insensitive) exists for the same creator.
     * Used to prevent the same user from creating duplicate group names.
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM UserGroup g " +
           "WHERE LOWER(g.name) = LOWER(:name) AND g.createdBy = :createdBy AND g.isActive = true")
    boolean existsByNameIgnoreCaseAndCreatedByAndIsActiveTrue(
            @Param("name") String name,
            @Param("createdBy") String createdBy);
}
