-- Genres table
CREATE TABLE IF NOT EXISTS genres (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Content base table (for common fields between movies and shows)
CREATE TABLE IF NOT EXISTS content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    release_year INTEGER,
    duration_minutes INTEGER,
    thumbnail_url VARCHAR(512),
    backdrop_url VARCHAR(512),
    content_rating VARCHAR(10),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Movies table
CREATE TABLE IF NOT EXISTS movies (
    id BIGINT PRIMARY KEY REFERENCES content(id) ON DELETE CASCADE,
    director VARCHAR(100),
    imdb_rating NUMERIC(3,1),
    box_office_revenue DECIMAL(15,2)
);

-- TV Shows table
CREATE TABLE IF NOT EXISTS tv_shows (
    id BIGINT PRIMARY KEY REFERENCES content(id) ON DELETE CASCADE,
    number_of_seasons INTEGER DEFAULT 1,
    number_of_episodes INTEGER DEFAULT 0,
    is_ongoing BOOLEAN DEFAULT true
);

-- Content Genres (many-to-many relationship)
CREATE TABLE IF NOT EXISTS content_genres (
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (content_id, genre_id)
);

-- Seasons table (for TV shows)
CREATE TABLE IF NOT EXISTS seasons (
    id BIGSERIAL PRIMARY KEY,
    tv_show_id BIGINT NOT NULL REFERENCES tv_shows(id) ON DELETE CASCADE,
    season_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    release_date DATE,
    poster_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tv_show_id, season_number)
);

-- Episodes table
CREATE TABLE IF NOT EXISTS episodes (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    episode_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER,
    thumbnail_url VARCHAR(512),
    video_url VARCHAR(512),
    release_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(season_id, episode_number)
);

-- Content Rating table
CREATE TABLE IF NOT EXISTS content_ratings (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    rating NUMERIC(2,1) NOT NULL CHECK (rating >= 0 AND rating <= 10),
    review TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(content_id, user_id)
);

-- Content View History
CREATE TABLE IF NOT EXISTS view_history (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    episode_id BIGINT REFERENCES episodes(id) ON DELETE SET NULL,
    progress_seconds INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN DEFAULT false,
    last_watched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(content_id, user_id, episode_id)
);

-- Indexes for better query performance
CREATE INDEX idx_content_title ON content(title);
CREATE INDEX idx_content_release_year ON content(release_year);
CREATE INDEX idx_content_rating ON content_ratings(content_id, rating);
CREATE INDEX idx_view_history_user ON view_history(user_id, last_watched_at);

-- Insert some initial genres
INSERT INTO genres (name, description) VALUES
    ('Action', 'High-energy, physical stunts and chases.'),
    ('Comedy', 'Designed to be humorous or amusing.'),
    ('Drama', 'Serious, plot-driven content.'),
    ('Sci-Fi', 'Futuristic, science-based themes.'),
    ('Horror', 'Designed to scare or frighten the audience.'),
    ('Romance', 'Focuses on romantic love.'),
    ('Documentary', 'Non-fictional, informative content.'),
    ('Animation', 'Animated content for all ages.')
ON CONFLICT (name) DO NOTHING;
