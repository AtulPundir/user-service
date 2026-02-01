package com.myapp.identityservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myapp.identityservice.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final String subscriptionServiceApiKey;
    private final String internalApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(
            @Value("${app.subscription-service.api-key}") String subscriptionServiceApiKey,
            @Value("${app.internal.api-key:${app.subscription-service.api-key}}") String internalApiKey,
            ObjectMapper objectMapper) {
        this.subscriptionServiceApiKey = subscriptionServiceApiKey;
        this.internalApiKey = internalApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/webhooks")) {
            authenticateWithApiKey(request, response, filterChain, subscriptionServiceApiKey);
        } else if (path.startsWith("/internal/")) {
            authenticateWithApiKey(request, response, filterChain, internalApiKey);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void authenticateWithApiKey(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain filterChain, String expectedKey)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(apiKey)) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No API key provided");
            return;
        }

        if (!apiKey.equals(expectedKey)) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "service",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
