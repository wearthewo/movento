CREATE TABLE IF NOT EXISTS recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL
);
