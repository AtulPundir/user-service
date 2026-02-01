package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.IdentityType;
import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.domain.UserStatus;
import com.myapp.identityservice.dto.request.ResolveOrCreateUserRequest;
import com.myapp.identityservice.dto.response.ResolveOrCreateUserResponse;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.CuidGenerator;
import com.myapp.identityservice.util.IdentifierNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceholderUserService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceholderUserService.class);

    private final UserRepository userRepository;
    private final CuidGenerator cuidGenerator;
    private final IdentifierNormalizer identifierNormalizer;
    private final ContactAliasService contactAliasService;

    public PlaceholderUserService(UserRepository userRepository,
                                   CuidGenerator cuidGenerator,
                                   IdentifierNormalizer identifierNormalizer,
                                   ContactAliasService contactAliasService) {
        this.userRepository = userRepository;
        this.cuidGenerator = cuidGenerator;
        this.identifierNormalizer = identifierNormalizer;
        this.contactAliasService = contactAliasService;
    }

    @Transactional
    public ResolveOrCreateUserResponse resolveOrCreate(ResolveOrCreateUserRequest request, String requestingUserId) {
        String normalizedKey = identifierNormalizer.normalize(
                request.getIdentityKey(), request.getIdentityType());

        // Check if user exists by identity_key
        User existing = userRepository.findByIdentityKey(normalizedKey).orElse(null);

        if (existing != null) {
            // Save alias if provided
            if (request.getAliasName() != null && requestingUserId != null) {
                contactAliasService.setAliasInternal(requestingUserId, existing.getId(), request.getAliasName());
            }

            return new ResolveOrCreateUserResponse(
                    existing.getId(),
                    existing.isVerified(),
                    false
            );
        }

        // Also check legacy columns for backward compat
        User legacyUser = null;
        if (request.getIdentityType() == IdentityType.PHONE) {
            legacyUser = userRepository.findByPhone(normalizedKey).orElse(null);
        } else {
            legacyUser = userRepository.findByEmail(normalizedKey).orElse(null);
        }

        if (legacyUser != null) {
            // Backfill identity_key on existing user
            if (legacyUser.getIdentityKey() == null) {
                legacyUser.setIdentityKey(normalizedKey);
                legacyUser.setIdentityType(request.getIdentityType());
                userRepository.save(legacyUser);
            }

            if (request.getAliasName() != null && requestingUserId != null) {
                contactAliasService.setAliasInternal(requestingUserId, legacyUser.getId(), request.getAliasName());
            }

            return new ResolveOrCreateUserResponse(
                    legacyUser.getId(),
                    legacyUser.isVerified(),
                    false
            );
        }

        // Create placeholder user — handle concurrent insert race via unique constraint
        try {
            User placeholder = new User();
            placeholder.setId(cuidGenerator.generate());
            placeholder.setIdentityKey(normalizedKey);
            placeholder.setIdentityType(request.getIdentityType());
            placeholder.setVerified(false);
            placeholder.setStatus(UserStatus.ACTIVE);
            placeholder.setDefaultMonthlyTaskLimit(0);

            if (request.getIdentityType() == IdentityType.PHONE) {
                placeholder.setPhone(normalizedKey);
            } else {
                placeholder.setEmail(normalizedKey);
            }

            User saved = userRepository.save(placeholder);
            logger.info("Created placeholder user: id={}, identityKey={}, type={}",
                    saved.getId(), maskKey(normalizedKey), request.getIdentityType());

            if (request.getAliasName() != null && requestingUserId != null) {
                contactAliasService.setAliasInternal(requestingUserId, saved.getId(), request.getAliasName());
            }

            return new ResolveOrCreateUserResponse(saved.getId(), false, true);
        } catch (DataIntegrityViolationException e) {
            // Concurrent insert race — another request created the user first, retry lookup
            logger.info("Concurrent placeholder creation detected for {}, retrying lookup", maskKey(normalizedKey));
            User raceWinner = userRepository.findByIdentityKey(normalizedKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "User not found after constraint violation for key: " + maskKey(normalizedKey)));

            if (request.getAliasName() != null && requestingUserId != null) {
                contactAliasService.setAliasInternal(requestingUserId, raceWinner.getId(), request.getAliasName());
            }

            return new ResolveOrCreateUserResponse(raceWinner.getId(), raceWinner.isVerified(), false);
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 4) return "***";
        return key.substring(0, 3) + "***" + key.substring(key.length() - 2);
    }
}
