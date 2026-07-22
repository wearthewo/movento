ALTER TABLE content ADD COLUMN IF NOT EXISTS slug VARCHAR(180);
ALTER TABLE content ADD COLUMN IF NOT EXISTS is_featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE content ADD COLUMN IF NOT EXISTS is_trending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE content ADD COLUMN IF NOT EXISTS language VARCHAR(10) NOT NULL DEFAULT 'en';
ALTER TABLE content ADD COLUMN IF NOT EXISTS content_type VARCHAR(31);
UPDATE content SET content_type = CASE WHEN EXISTS (SELECT 1 FROM movies m WHERE m.id = content.id) THEN 'Movie' ELSE 'TvShow' END WHERE content_type IS NULL;
ALTER TABLE content ALTER COLUMN content_type SET NOT NULL;
UPDATE content SET slug = lower(regexp_replace(title, '[^a-zA-Z0-9]+', '-', 'g')) || '-' || id WHERE slug IS NULL;
ALTER TABLE content ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_content_slug ON content(slug);
CREATE INDEX IF NOT EXISTS idx_content_active_created ON content(is_active, created_at DESC);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_content_title_trgm ON content USING gin(title gin_trgm_ops);

CREATE TABLE IF NOT EXISTS media_assets (
  id UUID PRIMARY KEY,
  content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  episode_id BIGINT REFERENCES episodes(id) ON DELETE CASCADE,
  hls_manifest_key VARCHAR(512) NOT NULL,
  trailer_url VARCHAR(512),
  duration_seconds INTEGER NOT NULL,
  captions JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
