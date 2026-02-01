package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.ContactAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactAliasRepository extends JpaRepository<ContactAlias, String> {

    Optional<ContactAlias> findByOwnerUserIdAndTargetUserId(String ownerUserId, String targetUserId);

    List<ContactAlias> findByOwnerUserId(String ownerUserId);

    @Query("SELECT ca FROM ContactAlias ca WHERE ca.ownerUserId = :ownerUserId AND ca.targetUserId IN :targetUserIds")
    List<ContactAlias> findByOwnerUserIdAndTargetUserIdIn(
            @Param("ownerUserId") String ownerUserId,
            @Param("targetUserIds") List<String> targetUserIds);
}
