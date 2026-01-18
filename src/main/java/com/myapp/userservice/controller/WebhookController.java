package com.myapp.userservice.controller;

import com.myapp.userservice.dto.request.SubscriptionWebhookRequest;
import com.myapp.userservice.dto.response.ApiResponse;
import com.myapp.userservice.exception.BadRequestException;
import com.myapp.userservice.service.UsageService;
import com.myapp.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Webhook endpoints for external services")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private static final int FREE_TIER_LIMIT = 50;

    private final UserService userService;
    private final UsageService usageService;

    public WebhookController(UserService userService, UsageService usageService) {
        this.userService = userService;
        this.usageService = usageService;
    }

    @PostMapping("/subscription")
    @Operation(summary = "Handle subscription webhook events")
    public ResponseEntity<Map<String, Object>> handleSubscriptionWebhook(
            @Valid @RequestBody SubscriptionWebhookRequest request) {

        logger.info("Received subscription webhook: eventType={}, authUserId={}",
                request.getEventType(), request.getAuthUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        switch (request.getEventType()) {
            case SUBSCRIPTION_ACTIVATED:
            case SUBSCRIPTION_UPDATED:
                handleSubscriptionUpdate(request, response);
                break;
            case SUBSCRIPTION_CANCELLED:
                handleSubscriptionCancelled(request, response);
                break;
            default:
                throw new BadRequestException("Unknown event type: " + request.getEventType());
        }

        return ResponseEntity.ok(response);
    }

    private void handleSubscriptionUpdate(SubscriptionWebhookRequest request, Map<String, Object> response) {
        if (request.getPlanId() == null || request.getMonthlyLimit() == null) {
            throw new BadRequestException("planId and monthlyLimit are required for subscription activation/update");
        }

        // Update user subscription
        userService.updateSubscription(request.getAuthUserId(), request.getPlanId(), request.getMonthlyLimit());

        // Update current month's limit
        var user = userService.findByAuthUserId(request.getAuthUserId());
        if (user != null) {
            usageService.updateMonthlyLimit(user.getId(), request.getMonthlyLimit());
        }

        response.put("message", request.getEventType() == SubscriptionWebhookRequest.EventType.SUBSCRIPTION_ACTIVATED
                ? "Subscription activated successfully"
                : "Subscription updated successfully");
        response.put("userId", user != null ? user.getId() : request.getAuthUserId());
        response.put("planId", request.getPlanId());
        response.put("monthlyLimit", request.getMonthlyLimit());

        logger.info("Subscription {}: userId={}, planId={}, limit={}",
                request.getEventType() == SubscriptionWebhookRequest.EventType.SUBSCRIPTION_ACTIVATED ? "activated" : "updated",
                request.getAuthUserId(), request.getPlanId(), request.getMonthlyLimit());
    }

    private void handleSubscriptionCancelled(SubscriptionWebhookRequest request, Map<String, Object> response) {
        // Revert to free tier
        userService.updateSubscription(request.getAuthUserId(), null, FREE_TIER_LIMIT);

        // Update current month's limit to free tier
        var user = userService.findByAuthUserId(request.getAuthUserId());
        if (user != null) {
            usageService.updateMonthlyLimit(user.getId(), FREE_TIER_LIMIT);
        }

        response.put("message", "Subscription cancelled successfully");
        response.put("userId", user != null ? user.getId() : request.getAuthUserId());
        response.put("planId", "FREE");
        response.put("monthlyLimit", FREE_TIER_LIMIT);

        logger.info("Subscription cancelled: userId={}, reverted to free tier", request.getAuthUserId());
    }
}
