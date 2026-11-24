-- V4 seeded "Afterlight" with a mistyped Unsplash photo id (13 characters instead of 12), so the image
-- request 404s. Earlier migrations must not be edited (Flyway checksums), so correct the data here.
UPDATE content
SET thumbnail_url = replace(thumbnail_url, 'photo-1500534314209-a25ddb2bd4297', 'photo-1500534314209-a25ddb2bd429'),
    backdrop_url  = replace(backdrop_url,  'photo-1500534314209-a25ddb2bd4297', 'photo-1500534314209-a25ddb2bd429')
WHERE thumbnail_url LIKE '%a25ddb2bd4297%' OR backdrop_url LIKE '%a25ddb2bd4297%';
