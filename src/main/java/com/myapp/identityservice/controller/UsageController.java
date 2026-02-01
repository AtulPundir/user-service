package com.myapp.identityservice.controller;

import com.myapp.identityservice.dto.request.UsageCheckRequest;
import com.myapp.identityservice.dto.request.UsageConsumeRequest;
import com.myapp.identityservice.dto.response.ApiResponse;
import com.myapp.identityservice.dto.response.UsageCheckResponse;
import com.myapp.identityservice.dto.response.UsageConsumeResponse;
import com.myapp.identityservice.dto.response.UsageResponse;
import com.myapp.identityservice.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/users/{id}/usage")
@Tag(name = "Usage", description = "Usage tracking endpoints")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/check")
    @Operation(summary = "Check if usage is allowed")
    public ResponseEntity<ApiResponse<UsageCheckResponse>> checkUsage(
            @PathVariable String id,
            @Valid @RequestBody UsageCheckRequest request) {
        UsageCheckResponse result = usageService.checkUsage(id, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/consume")
    @Operation(summary = "Consume usage")
    public ResponseEntity<ApiResponse<UsageConsumeResponse>> consumeUsage(
            @PathVariable String id,
            @Valid @RequestBody UsageConsumeRequest request) {
        UsageConsumeResponse result = usageService.consumeUsage(id, request.getAmount(), request.getIdempotencyKey());
        return ResponseEntity.ok(ApiResponse.success(result, "Usage consumed successfully"));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current month usage")
    public ResponseEntity<ApiResponse<UsageResponse>> getCurrentMonthUsage(@PathVariable String id) {
        UsageResponse usage = usageService.getCurrentMonthUsage(id);
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get monthly usage for specific year and month")
    public ResponseEntity<ApiResponse<UsageResponse>> getMonthlyUsage(
            @PathVariable String id,
            @PathVariable int year,
            @PathVariable int month) {
        UsageResponse usage = usageService.getMonthlyUsage(id, year, month);
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    @GetMapping
    @Operation(summary = "Get yearly usage")
    public ResponseEntity<ApiResponse<List<UsageResponse>>> getYearlyUsage(
            @PathVariable String id,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<UsageResponse> usage = usageService.getYearlyUsage(id, targetYear);
        return ResponseEntity.ok(ApiResponse.success(usage));
    }
}
