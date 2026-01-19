-- Migration V3: Add group invitations table for invite-based placeholder pattern
-- This table stores invitations for users who haven't signed up yet

CREATE TABLE invitations (
    id VARCHAR(30) PRIMARY KEY,
    group_id VARCHAR(30) NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    identifier VARCHAR(255) NOT NULL,
    identifier_type VARCHAR(10) NOT NULL CHECK (identifier_type IN ('EMAIL', 'PHONE')),
    invitee_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED')),
    invited_by VARCHAR(30) NOT NULL,
    resolved_user_id VARCHAR(30) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for efficient querying
CREATE INDEX idx_invitation_group_id ON invitations(group_id);
CREATE INDEX idx_invitation_identifier ON invitations(identifier);
CREATE INDEX idx_invitation_status ON invitations(status);
CREATE INDEX idx_invitation_identifier_status ON invitations(identifier, status);
CREATE INDEX idx_invitation_expires_at ON invitations(expires_at);

-- Unique constraint: Only one pending invitation per identifier per group
CREATE UNIQUE INDEX idx_invitation_unique_pending
ON invitations(identifier, group_id)
WHERE status = 'PENDING';

-- Comment explaining the table's purpose
COMMENT ON TABLE invitations IS 'Stores invitations for non-registered users to join groups. When a user signs up, pending invitations matching their email/phone are resolved.';
COMMENT ON COLUMN invitations.identifier IS 'Email or phone number used to match the invitation when user signs up';
COMMENT ON COLUMN invitations.identifier_type IS 'Type of identifier: EMAIL or PHONE';
COMMENT ON COLUMN invitations.resolved_user_id IS 'The user ID assigned after signup, set when status changes to ACCEPTED';
