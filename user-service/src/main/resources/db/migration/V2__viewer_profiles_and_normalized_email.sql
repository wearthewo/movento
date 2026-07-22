CREATE TABLE IF NOT EXISTS viewer_profiles (
 id UUID PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 name VARCHAR(40) NOT NULL,
 avatar_url VARCHAR(512),
 kids_mode BOOLEAN NOT NULL DEFAULT FALSE,
 maturity_level VARCHAR(20) NOT NULL DEFAULT 'ADULT',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,name)
);
CREATE INDEX IF NOT EXISTS idx_viewer_profiles_user ON viewer_profiles(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_normalized ON users(LOWER(email));
