INSERT INTO content (title, slug, description, release_year, duration_minutes, thumbnail_url, backdrop_url, content_rating, is_active, is_featured, is_trending, language, content_type)
VALUES
('Cosmos Beyond','cosmos-beyond','A lone cartographer crosses an uncharted galaxy and discovers a signal that changes humanity.',2026,118,'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=1800&q=90','PG-13',true,true,true,'en','Movie'),
('Afterlight','afterlight','Two strangers search for home in a city where every memory leaves a visible trace.',2024,104,'https://images.unsplash.com/photo-1500534314209-a25ddb2bd4297?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1500534314209-a25ddb2bd4297?auto=format&fit=crop&w=1800&q=90','R',true,false,false,'en','Movie'),
('Velocity','velocity','An underground racing crew gets one night to clear its name.',2025,111,'https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=1800&q=90','PG-13',true,false,true,'en','Movie')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO movies (id, director)
SELECT id, 'Movento Studio' FROM content WHERE slug IN ('cosmos-beyond','afterlight','velocity') ON CONFLICT (id) DO NOTHING;

INSERT INTO content_genres (content_id, genre_id)
SELECT c.id, g.id FROM content c JOIN genres g ON
 (c.slug='cosmos-beyond' AND g.name='Sci-Fi') OR
 (c.slug='afterlight' AND g.name='Drama') OR
 (c.slug='velocity' AND g.name='Action')
ON CONFLICT DO NOTHING;
