-- Migration V6: Add generic context-aware invitations table
-- Supports invitations for any domain: TRIP, COLLABORATION, AGREEMENT, EXPENSE_GROUP

CREATE TABLE context_invitations (
    id VARCHAR(30) PRIMARY KEY,
    invited_by VARCHAR(30) NOT NULL,
    target_user_id VARCHAR(30) NOT NULL,
    context_type VARCHAR(20) NOT NULL,
    context_id VARCHAR(50) NOT NULL,
    context_role VARCHAR(20) DEFAULT 'MEMBER',
    channel VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    alias_name VARCHAR(255),
    message TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT chk_ctx_inv_context_type
        CHECK (context_type IN ('TRIP', 'COLLABORATION', 'AGREEMENT', 'EXPENSE_GROUP')),
    CONSTRAINT chk_ctx_inv_channel
        CHECK (channel IN ('SMS', 'EMAIL', 'IN_APP')),
    CONSTRAINT chk_ctx_inv_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_ctx_inv_invited_by ON context_invitations(invited_by);
CREATE INDEX idx_ctx_inv_target_user ON context_invitations(target_user_id);
CREATE INDEX idx_ctx_inv_context ON context_invitations(context_type, context_id);
CREATE INDEX idx_ctx_inv_status ON context_invitations(status);
CREATE INDEX idx_ctx_inv_expires_at ON context_invitations(expires_at);

-- One pending invitation per target user per context
CREATE UNIQUE INDEX idx_ctx_inv_unique_pending
ON context_invitations(target_user_id, context_type, context_id)
WHERE status = 'PENDING';
