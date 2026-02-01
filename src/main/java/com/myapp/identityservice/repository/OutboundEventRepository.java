package com.myapp.identityservice.repository;

import com.myapp.identityservice.domain.OutboundEvent;
import com.myapp.identityservice.domain.OutboundEvent.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboundEventRepository extends JpaRepository<OutboundEvent, String> {

    @Query("SELECT e FROM OutboundEvent e WHERE e.status IN ('PENDING', 'FAILED') " +
           "AND e.nextRetryAt <= :now ORDER BY e.createdAt ASC")
    List<OutboundEvent> findEventsToDeliver(@Param("now") Instant now, Pageable pageable);

    List<OutboundEvent> findByStatus(EventStatus status);

    long countByStatus(EventStatus status);

    @Modifying
    @Query("DELETE FROM OutboundEvent e WHERE e.status = 'DELIVERED' AND e.updatedAt < :cutoff")
    int deleteDeliveredOlderThan(@Param("cutoff") Instant cutoff);
}
