-- Add created_by column to user_groups table
ALTER TABLE user_groups ADD COLUMN created_by VARCHAR(30);

-- Add index for created_by column
CREATE INDEX idx_group_created_by ON user_groups(created_by);

-- Add unique constraint: same user cannot create groups with the same name (case insensitive)
CREATE UNIQUE INDEX idx_group_name_created_by_unique
ON user_groups(LOWER(name), created_by)
WHERE is_active = true;
