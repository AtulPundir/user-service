-- Migration V5: Add contact aliases table
-- Stores viewer-specific names for contacts (e.g., "Husband", "Friend")

CREATE TABLE contact_aliases (
    id VARCHAR(30) PRIMARY KEY,
    owner_user_id VARCHAR(30) NOT NULL,
    target_user_id VARCHAR(30) NOT NULL,
    alias_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT uk_contact_alias_owner_target UNIQUE (owner_user_id, target_user_id)
);

CREATE INDEX idx_contact_aliases_owner ON contact_aliases(owner_user_id);
CREATE INDEX idx_contact_aliases_target ON contact_aliases(target_user_id);
