-- Behavioural signals consumed from streaming-service's playback.started events.
-- event_id is the idempotency key: redelivered messages hit ON CONFLICT DO NOTHING.
CREATE TABLE IF NOT EXISTS viewing_events (
    event_id    UUID PRIMARY KEY,
    account_id  BIGINT NOT NULL,
    profile_id  UUID,
    content_id  BIGINT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_viewing_events_profile_time ON viewing_events (profile_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_viewing_events_time_content ON viewing_events (occurred_at DESC, content_id);
