-- Seed seasons and episodes for demo series so title pages can show a Netflix-style episode list.
-- Idempotent: only shows without any season get generated data.
INSERT INTO seasons (tv_show_id, season_number, title, description, release_date)
SELECT t.id, s.n, 'Season ' || s.n, c.description, make_date(COALESCE(c.release_year, 2025) + s.n - 1, 1, 1)
FROM tv_shows t
JOIN content c ON c.id = t.id
CROSS JOIN LATERAL generate_series(1, GREATEST(COALESCE(t.number_of_seasons, 1), 1)) AS s(n)
WHERE NOT EXISTS (SELECT 1 FROM seasons existing WHERE existing.tv_show_id = t.id);

WITH episode_titles(n, title) AS (
  VALUES (1, 'Pilot'), (2, 'The Signal'), (3, 'Undertow'), (4, 'Crossing Lines'), (5, 'Static'),
         (6, 'Second Light'), (7, 'The Long Night'), (8, 'Homecoming'), (9, 'Fault Lines'), (10, 'Finale')
)
INSERT INTO episodes (season_id, episode_number, title, description, duration_minutes, thumbnail_url, release_date)
SELECT s.id,
       e.n,
       COALESCE(et.title, 'Chapter ' || e.n),
       'Episode ' || e.n || ' of ' || c.title || '. ' || COALESCE(c.description, ''),
       COALESCE(c.duration_minutes, 45),
       c.thumbnail_url,
       s.release_date + (e.n - 1) * INTERVAL '7 days'
FROM seasons s
JOIN tv_shows t ON t.id = s.tv_show_id
JOIN content c ON c.id = t.id
CROSS JOIN LATERAL generate_series(1, LEAST(GREATEST(COALESCE(NULLIF(t.number_of_episodes, 0), 6) / GREATEST(COALESCE(t.number_of_seasons, 1), 1), 1), 10)) AS e(n)
LEFT JOIN episode_titles et ON et.n = e.n
WHERE NOT EXISTS (SELECT 1 FROM episodes existing WHERE existing.season_id = s.id);

UPDATE tv_shows t
SET number_of_episodes = sub.cnt
FROM (SELECT s.tv_show_id, COUNT(e.id) AS cnt FROM seasons s JOIN episodes e ON e.season_id = s.id GROUP BY s.tv_show_id) sub
WHERE sub.tv_show_id = t.id;

-- Hot paths for Continue Watching and My List.
CREATE INDEX IF NOT EXISTS idx_playback_progress_profile_updated ON playback_progress(profile_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_watchlist_profile_created ON watchlist_items(profile_id, created_at DESC);
