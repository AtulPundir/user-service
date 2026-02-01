-- Migration V4: Add placeholder user support
-- Enables creating users without auth_user_id (placeholders for unregistered contacts)

-- Step 1: Make existing columns nullable for placeholder users
ALTER TABLE users ALTER COLUMN auth_user_id DROP NOT NULL;
ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;

-- Step 2: Drop existing unique constraints that conflict with NULLs
-- PostgreSQL allows multiple NULLs in UNIQUE columns, so these are fine as-is

-- Step 3: Add new identity columns
ALTER TABLE users ADD COLUMN identity_key VARCHAR(255);
ALTER TABLE users ADD COLUMN identity_type VARCHAR(10);

-- Step 4: Backfill identity_key for existing users (prefer phone, fallback to email)
UPDATE users
SET identity_key = COALESCE(phone, LOWER(TRIM(email))),
    identity_type = CASE
        WHEN phone IS NOT NULL THEN 'PHONE'
        ELSE 'EMAIL'
    END
WHERE identity_key IS NULL;

-- Step 5: Make new columns NOT NULL after backfill
ALTER TABLE users ALTER COLUMN identity_key SET NOT NULL;
ALTER TABLE users ALTER COLUMN identity_type SET NOT NULL;

-- Step 6: Add unique constraint and index
ALTER TABLE users ADD CONSTRAINT uk_users_identity_key UNIQUE (identity_key);
CREATE INDEX idx_users_identity_type ON users(identity_type);

-- Step 7: Add check constraint
ALTER TABLE users ADD CONSTRAINT chk_users_identity_type
    CHECK (identity_type IN ('EMAIL', 'PHONE'));
