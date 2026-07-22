INSERT INTO genres (name, description) VALUES
  ('Adventure', 'Journeys, discovery, and high-stakes exploration.'),
  ('Thriller', 'Suspenseful stories with danger and momentum.'),
  ('Mystery', 'Stories built around secrets, clues, and reveals.'),
  ('Nature', 'Wildlife, landscapes, and the natural world.'),
  ('Family', 'Accessible stories for shared viewing.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO content (title, slug, description, release_year, duration_minutes, thumbnail_url, backdrop_url, content_rating, is_active, is_featured, is_trending, language, content_type)
VALUES
  ('Deep Blue', 'deep-blue', 'Dive below the last frontier with the crew mapping Earth''s most mysterious ecosystems.', 2025, 48, 'https://images.unsplash.com/photo-1439405326854-014607f694d7?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1439405326854-014607f694d7?auto=format&fit=crop&w=1800&q=90', 'TV-PG', true, false, true, 'en', 'TvShow'),
  ('Wild North', 'wild-north', 'A year in the Arctic following the people and wildlife shaped by its changing seasons.', 2026, 52, 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1800&q=90', 'TV-G', true, false, false, 'en', 'TvShow'),
  ('Small Wonders', 'small-wonders', 'Animated stories about brave choices in a very large world.', 2026, 24, 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1800&q=90', 'TV-Y7', true, false, false, 'en', 'TvShow'),
  ('Neon Harbor', 'neon-harbor', 'A detective follows encrypted dreams through a rain-soaked city built on secrets.', 2026, 126, 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1800&q=90', 'TV-14', true, true, true, 'en', 'Movie'),
  ('The Last Orchard', 'the-last-orchard', 'Three siblings return to their family farm and uncover a buried promise from another generation.', 2023, 97, 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1800&q=90', 'PG', true, false, false, 'en', 'Movie'),
  ('Iron Meridian', 'iron-meridian', 'A rescue pilot crosses a militarized border to save a team trapped beneath the mountains.', 2025, 113, 'https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1800&q=90', 'PG-13', true, false, true, 'en', 'Movie'),
  ('Laugh Track', 'laugh-track', 'A washed-up sitcom room tries to save its final season while accidentally becoming a family.', 2024, 31, 'https://images.unsplash.com/photo-1527224857830-43a7acc85260?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1527224857830-43a7acc85260?auto=format&fit=crop&w=1800&q=90', 'TV-14', true, false, false, 'en', 'TvShow'),
  ('Signal House', 'signal-house', 'A remote lighthouse begins receiving broadcasts from events that have not happened yet.', 2026, 50, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=90', 'TV-14', true, false, true, 'en', 'TvShow'),
  ('Paper Suns', 'paper-suns', 'An artist and an astronomer build a map of impossible skies after a town loses daylight.', 2024, 108, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=1800&q=90', 'PG-13', true, false, false, 'en', 'Movie'),
  ('Street Food Atlas', 'street-food-atlas', 'Chefs, vendors, and night markets tell the story of cities through food.', 2025, 43, 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=90', 'TV-PG', true, false, true, 'en', 'TvShow')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO movies (id, director, imdb_rating, box_office_revenue)
SELECT id, 'Movento Studio', 8.1, 0 FROM content
WHERE slug IN ('neon-harbor', 'the-last-orchard', 'iron-meridian', 'paper-suns')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tv_shows (id, number_of_seasons, number_of_episodes, is_ongoing)
SELECT id, 1, 8, true FROM content
WHERE slug IN ('deep-blue', 'wild-north', 'small-wonders', 'laugh-track', 'signal-house', 'street-food-atlas')
ON CONFLICT (id) DO NOTHING;

INSERT INTO content_genres (content_id, genre_id)
SELECT c.id, g.id
FROM content c
JOIN genres g ON
  (c.slug = 'deep-blue' AND g.name IN ('Documentary', 'Nature')) OR
  (c.slug = 'wild-north' AND g.name IN ('Documentary', 'Nature')) OR
  (c.slug = 'small-wonders' AND g.name IN ('Animation', 'Family')) OR
  (c.slug = 'neon-harbor' AND g.name IN ('Mystery', 'Sci-Fi', 'Thriller')) OR
  (c.slug = 'the-last-orchard' AND g.name IN ('Drama', 'Family')) OR
  (c.slug = 'iron-meridian' AND g.name IN ('Action', 'Adventure', 'Thriller')) OR
  (c.slug = 'laugh-track' AND g.name IN ('Comedy', 'Drama')) OR
  (c.slug = 'signal-house' AND g.name IN ('Mystery', 'Sci-Fi')) OR
  (c.slug = 'paper-suns' AND g.name IN ('Drama', 'Romance', 'Sci-Fi')) OR
  (c.slug = 'street-food-atlas' AND g.name IN ('Documentary'))
ON CONFLICT DO NOTHING;
