package com.myapp.identityservice.controller;

import com.myapp.identityservice.domain.OutboundEvent;
import com.myapp.identityservice.domain.OutboundEvent.EventStatus;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.repository.OutboundEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/admin/outbox")
@Tag(name = "Outbox Admin", description = "Admin endpoints for outbound event inspection and retry")
public class OutboxAdminController {

    private final OutboundEventRepository outboundEventRepository;

    public OutboxAdminController(OutboundEventRepository outboundEventRepository) {
        this.outboundEventRepository = outboundEventRepository;
    }

    @GetMapping("/failed")
    @Operation(summary = "List permanently failed outbound events")
    public ResponseEntity<ApiResponse<List<OutboundEvent>>> getFailedEvents() {
        List<OutboundEvent> failed = outboundEventRepository.findByStatus(EventStatus.PERMANENTLY_FAILED);
        return ResponseEntity.ok(ApiResponse.success(failed, "Found " + failed.size() + " permanently failed events"));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Reset a permanently failed event for redelivery")
    public ResponseEntity<ApiResponse<String>> retryEvent(@PathVariable String id) {
        OutboundEvent event = outboundEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        event.resetForRetry();
        outboundEventRepository.save(event);

        return ResponseEntity.ok(ApiResponse.success("OK",
                "Event " + event.getEventId() + " reset for redelivery"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get outbox event counts by status")
    public ResponseEntity<ApiResponse<OutboxStats>> getStats() {
        OutboxStats stats = new OutboxStats(
                outboundEventRepository.countByStatus(EventStatus.PENDING),
                outboundEventRepository.countByStatus(EventStatus.FAILED),
                outboundEventRepository.countByStatus(EventStatus.DELIVERED),
                outboundEventRepository.countByStatus(EventStatus.PERMANENTLY_FAILED)
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Outbox statistics"));
    }

    public record OutboxStats(long pending, long failed, long delivered, long permanentlyFailed) {}
}
