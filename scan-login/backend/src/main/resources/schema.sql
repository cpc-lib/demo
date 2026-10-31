-- Applied automatically on every boot (spring.sql.init.mode=always); all
-- statements are idempotent so existing databases are left untouched.
CREATE TABLE IF NOT EXISTS app_user
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    -- Bumped to invalidate every issued token of the user instantly (kick).
    token_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS login_audit
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    ip VARCHAR(45),
    user_agent VARCHAR(255),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS refresh_token
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    client VARCHAR(8) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_refresh_token_user (user_id)
);

-- MySQL lacks ADD COLUMN IF NOT EXISTS; rerunning this on a migrated database
-- raises a duplicate-column error which spring.sql.init.continue-on-error absorbs.
ALTER TABLE app_user ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
