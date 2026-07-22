ALTER TABLE streaming_sessions ADD COLUMN IF NOT EXISTS profile_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE streaming_sessions ADD COLUMN IF NOT EXISTS progress_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE streaming_sessions ADD COLUMN IF NOT EXISTS completed BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_streaming_profile_recent ON streaming_sessions(user_id,profile_id,started_at DESC);
