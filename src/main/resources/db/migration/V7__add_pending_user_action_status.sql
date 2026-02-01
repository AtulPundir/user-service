-- Add PENDING_USER_ACTION status for invitations requiring explicit user consent after verification
-- State machine: PENDING → PENDING_USER_ACTION (on verification, if policy = REQUIRE_USER_CONFIRMATION)
--                PENDING_USER_ACTION → ACCEPTED (user explicitly accepts)
--                PENDING_USER_ACTION → REJECTED (user explicitly rejects)
--                PENDING_USER_ACTION → EXPIRED (expiry timer)

-- Update CHECK constraint to allow PENDING_USER_ACTION
ALTER TABLE context_invitations DROP CONSTRAINT IF EXISTS context_invitations_status_check;
ALTER TABLE context_invitations ADD CONSTRAINT context_invitations_status_check
    CHECK (status IN ('PENDING', 'PENDING_USER_ACTION', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'));

-- Extend partial unique index to cover PENDING_USER_ACTION
-- (prevent duplicate active invitations for same user+context)
DROP INDEX IF EXISTS idx_ctx_inv_unique_pending;
CREATE UNIQUE INDEX idx_ctx_inv_unique_pending
    ON context_invitations (target_user_id, context_type, context_id)
    WHERE status IN ('PENDING', 'PENDING_USER_ACTION');

-- Also expire PENDING_USER_ACTION invitations that have passed their expiry
-- (the expireOverdueInvitations query will be updated in code to include this status)
