package com.myapp.identityservice.service;

import com.myapp.identityservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled invariant checker that detects identity linking anomalies.
 *
 * INVARIANTS enforced:
 * 1. Every verified user MUST have auth_user_id set
 * 2. Every verified user MUST have identity_key set
 * 3. No two users should share the same auth_user_id
 *
 * This is a DETECTION mechanism, not a repair mechanism.
 * Violations are logged at ERROR level for alerting.
 */
@Component
public class IdentityInvariantChecker {

    private static final Logger logger = LoggerFactory.getLogger(IdentityInvariantChecker.class);

    private final UserRepository userRepository;

    public IdentityInvariantChecker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Runs every hour. Detects invariant violations and logs at ERROR level.
     */
    @Scheduled(cron = "${app.invariant-check.cron:0 0 * * * *}")
    public void checkInvariants() {
        logger.info("Running identity invariant check...");

        long verifiedWithoutAuthId = userRepository.countVerifiedWithoutAuthUserId();
        long verifiedWithoutIdentityKey = userRepository.countVerifiedWithoutIdentityKey();
        long duplicateAuthUserIds = userRepository.countDuplicateAuthUserIds();

        if (verifiedWithoutAuthId > 0) {
            logger.error("INVARIANT VIOLATION: {} verified users have NO auth_user_id. " +
                    "These users logged in via OTP but identity-service failed to link them. " +
                    "Run GET /internal/admin/identity/violations to inspect.",
                    verifiedWithoutAuthId);
        }

        if (verifiedWithoutIdentityKey > 0) {
            logger.error("INVARIANT VIOLATION: {} verified users have NO identity_key. " +
                    "This indicates a data corruption issue.",
                    verifiedWithoutIdentityKey);
        }

        if (duplicateAuthUserIds > 0) {
            logger.error("INVARIANT VIOLATION: {} auth_user_ids are linked to multiple user_db rows. " +
                    "This indicates a deduplication failure.",
                    duplicateAuthUserIds);
        }

        if (verifiedWithoutAuthId == 0 && verifiedWithoutIdentityKey == 0 && duplicateAuthUserIds == 0) {
            logger.info("Identity invariant check passed: no violations found.");
        }
    }
}
