-- User Service Database Schema
-- Migration V1: Initial schema creation

-- Users table
CREATE TABLE users (
    id VARCHAR(30) PRIMARY KEY,
    auth_user_id VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    default_monthly_task_limit INT DEFAULT 50 NOT NULL,
    subscription_plan_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- Users indexes
CREATE INDEX idx_auth_user_id ON users(auth_user_id);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_phone ON users(phone);
CREATE INDEX idx_status ON users(status);
CREATE INDEX idx_is_verified ON users(is_verified);
CREATE INDEX idx_subscription_plan_id ON users(subscription_plan_id);

-- User monthly usage table
CREATE TABLE user_monthly_usage (
    id VARCHAR(30) PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year INT NOT NULL,
    month INT NOT NULL CHECK (month >= 1 AND month <= 12),
    monthly_limit INT NOT NULL,
    utilised INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, year, month)
);

-- User monthly usage indexes
CREATE INDEX idx_usage_user_id ON user_monthly_usage(user_id);
CREATE INDEX idx_usage_year_month ON user_monthly_usage(year, month);
CREATE INDEX idx_usage_user_year_month ON user_monthly_usage(user_id, year, month);

-- User groups table
CREATE TABLE user_groups (
    id VARCHAR(30) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_group_id VARCHAR(30) REFERENCES user_groups(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- User groups indexes
CREATE INDEX idx_group_parent_group_id ON user_groups(parent_group_id);
CREATE INDEX idx_group_is_active ON user_groups(is_active);
CREATE INDEX idx_group_name ON user_groups(name);

-- User group memberships table (audit log, not join table)
CREATE TABLE user_group_memberships (
    id VARCHAR(30) PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id VARCHAR(30) NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('ADDED', 'REMOVED')),
    performed_by VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- User group memberships indexes
CREATE INDEX idx_membership_user_id ON user_group_memberships(user_id);
CREATE INDEX idx_membership_group_id ON user_group_memberships(group_id);
CREATE INDEX idx_membership_user_group ON user_group_memberships(user_id, group_id);
CREATE INDEX idx_membership_created_at ON user_group_memberships(created_at);
