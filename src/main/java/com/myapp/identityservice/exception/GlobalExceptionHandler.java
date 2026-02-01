package com.myapp.identityservice.exception;

import com.myapp.identityservice.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:production}")
    private String activeProfile;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        logger.error("Application error: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUsageLimitExceeded(UsageLimitExceededException ex) {
        logger.warn("Usage limit exceeded: limit={}, utilised={}, requested={}",
                ex.getMonthlyLimit(), ex.getUtilised(), ex.getRequested());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        response.put("details", Map.of(
                "monthlyLimit", ex.getMonthlyLimit(),
                "utilised", ex.getUtilised(),
                "requested", ex.getRequested(),
                "remaining", ex.getRemaining()
        ));

        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("{\"field\":\"%s\",\"message\":\"%s\"}",
                        error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(",", "[", "]"));

        String message = "Validation failed: " + errors;
        logger.warn("Validation error: {}", message);

        ApiResponse<Object> response = ApiResponse.error(message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: ", ex);

        String message = "development".equals(activeProfile) ? ex.getMessage() : "Internal server error";
        ApiResponse<Object> response = ApiResponse.error(message);

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
