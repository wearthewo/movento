package com.movento.contentservice.catalog;

import com.movento.contentservice.dto.CatalogTitleResponse;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds the Netflix-style home feed (billboard + rails), title lists, episodes and "More like this".
 * All reads respect the viewer profile's maturity level.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private static final int RAIL_SIZE = 20;
    private static final int MIN_GENRE_RAIL = 3;

    private static final java.time.Duration HOME_TTL = java.time.Duration.ofSeconds(60);
    private static final int CATALOG_SCAN_LIMIT = 500;

    private final ContentRepository content;
    private final JdbcTemplate jdbc;

    /**
     * The home feed is the hottest read and identical for every viewer of a maturity level, so it is
     * memoised for a minute per level and dropped whenever an admin changes the catalog.
     */
    private final java.util.concurrent.ConcurrentHashMap<MaturityPolicy, CachedFeed> homeCache = new java.util.concurrent.ConcurrentHashMap<>();

    private record CachedFeed(HomeFeed feed, java.time.Instant expiresAt) {}

    public void invalidate() {
        homeCache.clear();
    }

    public record Rail(String id, String title, String kind, List<CatalogTitleResponse> items) {}
    public record HomeFeed(CatalogTitleResponse featured, List<Rail> rails) {}
    public record EpisodeResponse(String id, int episodeNumber, String title, String synopsis, Integer runtimeMinutes, String thumbnailUrl) {}
    public record SeasonResponse(int seasonNumber, String title, List<EpisodeResponse> episodes) {}

    public HomeFeed home(MaturityPolicy policy) {
        CachedFeed cached = homeCache.get(policy);
        if (cached != null && cached.expiresAt().isAfter(java.time.Instant.now())) return cached.feed();
        HomeFeed feed = buildHome(policy);
        homeCache.put(policy, new CachedFeed(feed, java.time.Instant.now().plus(HOME_TTL)));
        return feed;
    }

    private HomeFeed buildHome(MaturityPolicy policy) {
        List<Content> visible = visible(policy);
        if (visible.isEmpty()) throw new NoSuchElementException("Catalog is empty");

        Content hero = visible.stream().filter(Content::isFeatured).findFirst().orElse(visible.get(0));
        List<Rail> rails = new ArrayList<>();
        addRail(rails, "trending", "Trending Now", "standard", visible.stream().filter(Content::isTrending).toList());
        addRail(rails, "top10", "Top 10 on Movento Today", "top10", popularity(visible).stream().limit(10).toList());
        addRail(rails, "new", "New on Movento", "standard", visible.stream()
                .sorted(Comparator.comparing(Content::getReleaseYear, Comparator.nullsLast(Comparator.<Integer>reverseOrder()))).toList());
        addRail(rails, "movies", "Blockbuster Movies", "standard", visible.stream().filter(c -> c instanceof Movie).toList());
        addRail(rails, "series", "Binge-worthy Series", "standard", visible.stream().filter(c -> !(c instanceof Movie)).toList());

        Map<String, List<Content>> byGenre = new TreeMap<>();
        for (Content item : visible) {
            for (Genre genre : item.getGenres()) byGenre.computeIfAbsent(genre.getName(), k -> new ArrayList<>()).add(item);
        }
        byGenre.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_GENRE_RAIL)
                .sorted(Map.Entry.<String, List<Content>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .forEach(e -> addRail(rails, "genre-" + slug(e.getKey()), genreRailTitle(e.getKey()), "standard", e.getValue()));

        return new HomeFeed(map(hero), rails);
    }

    public List<CatalogTitleResponse> titles(MaturityPolicy policy, String type, String query, String genre) {
        List<Content> items = query == null || query.isBlank()
                ? visible(policy)
                : content.searchCatalog(query.trim(), org.springframework.data.domain.PageRequest.of(0, 100)).stream()
                        .filter(c -> policy.allows(c.getContentRating())).toList();
        Map<Long, Content> deduped = new LinkedHashMap<>();
        items.stream()
                .filter(item -> type == null || type.equalsIgnoreCase(typeOf(item)))
                .filter(item -> genre == null || item.getGenres().stream().anyMatch(g -> g.getName().equalsIgnoreCase(genre)))
                .forEach(item -> deduped.putIfAbsent(item.getId(), item));
        return deduped.values().stream().map(this::map).toList();
    }

    public CatalogTitleResponse title(MaturityPolicy policy, String slug) {
        return map(find(policy, slug));
    }

    public List<CatalogTitleResponse> byIds(MaturityPolicy policy, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Content> found = content.findByIds(ids).stream()
                .filter(Content::isActive)
                .filter(c -> policy.allows(c.getContentRating()))
                .collect(Collectors.toMap(Content::getId, c -> c));
        return ids.stream().map(found::get).filter(java.util.Objects::nonNull).map(this::map).toList();
    }

    /** Titles sharing the most genres with the given one, ties broken by trending and recency. */
    public List<CatalogTitleResponse> similar(MaturityPolicy policy, String slug) {
        Content base = find(policy, slug);
        Set<Long> genres = base.getGenres().stream().map(Genre::getId).collect(Collectors.toSet());
        return visible(policy).stream()
                .filter(c -> !c.getId().equals(base.getId()))
                .map(c -> Map.entry(c, c.getGenres().stream().filter(g -> genres.contains(g.getId())).count()))
                .filter(e -> e.getValue() > 0)
                .sorted(Comparator.<Map.Entry<Content, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing((Map.Entry<Content, Long> e) -> !e.getKey().isTrending())
                        .thenComparing((Map.Entry<Content, Long> e) -> e.getKey().getReleaseYear(), Comparator.nullsLast(Comparator.<Integer>reverseOrder())))
                .limit(12)
                .map(e -> map(e.getKey()))
                .toList();
    }

    public List<SeasonResponse> episodes(MaturityPolicy policy, String slug) {
        Content show = find(policy, slug);
        if (show instanceof Movie) return List.of();
        Map<Integer, SeasonResponse> seasons = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.season_number, s.title AS season_title, e.id, e.episode_number, e.title, e.description,
                       e.duration_minutes, e.thumbnail_url
                FROM seasons s JOIN episodes e ON e.season_id = s.id
                WHERE s.tv_show_id = ?
                ORDER BY s.season_number, e.episode_number
                """, rs -> {
            int number = rs.getInt("season_number");
            String seasonTitle = rs.getString("season_title");
            EpisodeResponse episode = new EpisodeResponse(
                    String.valueOf(rs.getLong("id")), rs.getInt("episode_number"), rs.getString("title"),
                    rs.getString("description"), (Integer) rs.getObject("duration_minutes"), rs.getString("thumbnail_url"));
            seasons.computeIfAbsent(number, n -> new SeasonResponse(n, seasonTitle, new ArrayList<>())).episodes().add(episode);
        }, show.getId());
        return List.copyOf(seasons.values());
    }

    private Content find(MaturityPolicy policy, String slug) {
        Content item = content.findBySlugAndActiveTrue(slug).orElseThrow();
        if (!policy.allows(item.getContentRating())) throw new NoSuchElementException(slug);
        return item;
    }

    private List<Content> visible(MaturityPolicy policy) {
        List<Long> ids = content.findActiveIdsNewestFirst(org.springframework.data.domain.PageRequest.of(0, CATALOG_SCAN_LIMIT));
        if (ids.isEmpty()) return List.of();
        Map<Long, Content> byId = content.findByIds(ids).stream().collect(Collectors.toMap(Content::getId, c -> c));
        return ids.stream().map(byId::get)
                .filter(java.util.Objects::nonNull)
                .filter(c -> policy.allows(c.getContentRating()))
                .toList();
    }

    private static List<Content> popularity(List<Content> items) {
        return items.stream()
                .sorted(Comparator.comparing((Content c) -> !c.isTrending())
                        .thenComparing(c -> !c.isFeatured())
                        .thenComparing(Content::getReleaseYear, Comparator.nullsLast(Comparator.<Integer>reverseOrder())))
                .toList();
    }

    private void addRail(List<Rail> rails, String id, String title, String kind, List<Content> items) {
        if (items.isEmpty()) return;
        rails.add(new Rail(id, title, kind, items.stream().limit(RAIL_SIZE).map(this::map).toList()));
    }

    private static String genreRailTitle(String genre) {
        return switch (genre) {
            case "Documentary" -> "Eye-opening Documentaries";
            case "Sci-Fi" -> "Sci-Fi & Fantasy";
            case "Family" -> "Watch Together";
            case "Thriller" -> "Edge-of-your-seat Thrillers";
            case "Drama" -> "Critically Acclaimed Dramas";
            case "Comedy" -> "Comedies to Lift Your Mood";
            default -> genre;
        };
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private static String typeOf(Content item) {
        return item instanceof Movie ? "MOVIE" : "SERIES";
    }

    public CatalogTitleResponse map(Content item) {
        return new CatalogTitleResponse(String.valueOf(item.getId()), item.getSlug(), item.getTitle(), item.getDescription(),
                typeOf(item), item.getReleaseYear(), item.getContentRating(), item.getDurationMinutes(),
                item.getGenres().stream().map(Genre::getName).sorted().toList(), item.getThumbnailUrl(), item.getBackdropUrl(),
                null, item.isFeatured(), item.isTrending());
    }
}
