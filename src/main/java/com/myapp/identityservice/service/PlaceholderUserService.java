package com.myapp.identityservice.service;

import com.myapp.identityservice.client.AuthServiceClient;
import com.myapp.identityservice.domain.IdentityType;
import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserStatus;
import com.myapp.identityservice.dto.request.ResolveOrCreateUserRequest;
import com.myapp.identityservice.dto.response.ResolveOrCreateUserResponse;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.IdentifierNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for resolving or creating placeholder users.
 *
 * NEW ARCHITECTURE: Auth-service is the single source of truth for user IDs.
 * When a contact is added:
 * 1. Call auth-service to resolve/create user → get userId
 * 2. Create/update user in user_db with SAME userId
 *
 * This ensures auth_db.users.id == user_db.users.id for all users.
 */
@Service
public class PlaceholderUserService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceholderUserService.class);

    private final UserRepository userRepository;
    private final IdentifierNormalizer identifierNormalizer;
    private final ContactAliasService contactAliasService;
    private final AuthServiceClient authServiceClient;

    public PlaceholderUserService(UserRepository userRepository,
                                   IdentifierNormalizer identifierNormalizer,
                                   ContactAliasService contactAliasService,
                                   AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.identifierNormalizer = identifierNormalizer;
        this.contactAliasService = contactAliasService;
        this.authServiceClient = authServiceClient;
    }

    @Transactional
    public ResolveOrCreateUserResponse resolveOrCreate(ResolveOrCreateUserRequest request, String requestingUserId) {
        String normalizedKey = identifierNormalizer.normalize(
                request.getIdentityKey(), request.getIdentityType());

        // Step 1: Call auth-service to resolve or create user
        // This ensures auth-service owns the user ID
        AuthServiceClient.ResolveOrCreateResponse authResponse = authServiceClient.resolveOrCreateUser(
                normalizedKey,
                request.getIdentityType().name(),
                request.getAliasName()
        );

        String userId = authResponse.userId();

        logger.info("Auth-service resolved user: userId={}, isVerified={}, isNew={}",
                userId, authResponse.isVerified(), authResponse.isNew());

        // Step 2: Ensure user exists in user_db with the SAME ID from auth-service
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            // User doesn't exist in user_db yet — create with same ID as auth-service
            user = createUserWithAuthId(userId, normalizedKey, request.getIdentityType(),
                    authResponse.isVerified(), authResponse.name());
        } else {
            // User exists — ensure data is in sync
            boolean needsSave = false;

            if (user.getIdentityKey() == null) {
                user.setIdentityKey(normalizedKey);
                user.setIdentityType(request.getIdentityType());
                needsSave = true;
            }

            // Sync verified status from auth-service (source of truth)
            if (authResponse.isVerified() && !user.isVerified()) {
                user.setVerified(true);
                needsSave = true;
            }

            // Ensure authUserId is set (should be same as id now)
            if (user.getAuthUserId() == null) {
                user.setAuthUserId(userId);
                needsSave = true;
            }

            if (needsSave) {
                user = userRepository.save(user);
            }
        }

        // Step 3: Save alias if provided
        if (request.getAliasName() != null && requestingUserId != null) {
            contactAliasService.setAliasInternal(requestingUserId, userId, request.getAliasName());
        }

        // Return the userId — same in both auth_db and user_db
        return new ResolveOrCreateUserResponse(userId, user.isVerified(), authResponse.isNew());
    }

    /**
     * Create user in user_db with the ID from auth-service.
     * This ensures auth_db.users.id == user_db.users.id
     */
    private User createUserWithAuthId(String authUserId, String identityKey, IdentityType identityType,
                                        boolean isVerified, String name) {
        try {
            User user = new User();
            user.setId(authUserId);           // SAME ID as auth-service
            user.setAuthUserId(authUserId);   // SAME ID
            user.setIdentityKey(identityKey);
            user.setIdentityType(identityType);
            user.setVerified(isVerified);
            user.setStatus(UserStatus.ACTIVE);
            user.setDefaultMonthlyTaskLimit(isVerified ? 50 : 0);
            user.setName(name);

            if (identityType == IdentityType.PHONE) {
                user.setPhone(identityKey);
            } else {
                user.setEmail(identityKey);
            }

            User saved = userRepository.saveAndFlush(user);
            logger.info("Created user in user_db with auth-service ID: id={}, identityKey={}, isVerified={}",
                    saved.getId(), maskKey(identityKey), isVerified);
            return saved;

        } catch (DataIntegrityViolationException e) {
            // Race condition — another request created the user, retry lookup
            logger.info("Concurrent user creation detected for {}, retrying lookup", maskKey(identityKey));
            return userRepository.findById(authUserId)
                    .orElseThrow(() -> new IllegalStateException(
                            "User not found after constraint violation for id: " + authUserId));
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 4) return "***";
        return key.substring(0, 3) + "***" + key.substring(key.length() - 2);
    }

    // ========================================================================================
    // DEPRECATED: Old methods that used different IDs across services.
    // Keeping for reference but no longer used.
    // ========================================================================================

    /*
    // OLD: This returned authUserId for linked users, but IDs were different
    private String effectiveUserId(User user) {
        return user.getAuthUserId() != null ? user.getAuthUserId() : user.getId();
    }

    // OLD: This created placeholder with identity-service generated ID
    private User createPlaceholderWithLocalId(String normalizedKey, IdentityType identityType) {
        User placeholder = new User();
        placeholder.setId(cuidGenerator.generate());  // Different from auth-service ID!
        placeholder.setIdentityKey(normalizedKey);
        placeholder.setIdentityType(identityType);
        placeholder.setVerified(false);
        placeholder.setStatus(UserStatus.ACTIVE);
        placeholder.setDefaultMonthlyTaskLimit(0);

        if (identityType == IdentityType.PHONE) {
            placeholder.setPhone(normalizedKey);
        } else {
            placeholder.setEmail(normalizedKey);
        }

        return userRepository.save(placeholder);
    }
    */
}
