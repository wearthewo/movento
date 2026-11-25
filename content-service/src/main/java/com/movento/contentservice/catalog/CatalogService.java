package com.movento.contentservice.catalog;

import com.movento.contentservice.dto.CatalogTitleResponse;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Genre;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.repository.ContentRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Builds the Netflix-style home feed (billboard + rails), title lists, episodes and "More like this".
 * All reads respect the viewer profile's maturity level.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private static final int RAIL_SIZE = 20;
    private static final int MIN_GENRE_RAIL = 3;

    private static final int CATALOG_SCAN_LIMIT = 500;

    /** How old a snapshot may get before a request asks for a background rebuild. */
    private static final Duration HOME_TTL = Duration.ofSeconds(60);
    /** Background refresh cadence for maturity levels that were used recently. */
    private static final Duration REFRESH_EVERY = Duration.ofSeconds(50);
    /** A level nobody asked for in this long stops being refreshed (no work for an idle stack). */
    private static final Duration KEEP_WARM_FOR = Duration.ofMinutes(10);
    /** Older than this, a snapshot is not served while it rebuilds; the request waits for fresh data. */
    private static final Duration MAX_STALE = Duration.ofMinutes(10);
    /** Rebuilds slower than this are logged at INFO, faster ones at DEBUG. */
    private static final long SLOW_REBUILD_MS = 300;

    private final ContentRepository content;
    private final JdbcTemplate jdbc;

    /**
     * Per maturity level: the visible catalog (titles with genres loaded) and the home feed built from it.
     * Browse, genre, "More like this", "Because you watched" and Home all read these read-only snapshots.
     *
     * <p>Stale-while-revalidate: snapshots are built at startup and refreshed in the background, so a request
     * never waits for a rebuild unless the service has just started or an admin has just changed the catalog.
     */
    private final Map<MaturityPolicy, Snapshot> snapshots = new ConcurrentHashMap<>();

    private record Snapshot(List<Content> items, HomeFeed feed, Instant builtAt) {}

    /** When each level was last requested, so idle levels are not rebuilt forever. */
    private final Map<MaturityPolicy, Instant> lastRequested = new ConcurrentHashMap<>();
    /** Levels with a rebuild already queued or running (avoids piling up duplicate rebuilds). */
    private final Set<MaturityPolicy> queued = ConcurrentHashMap.newKeySet();
    /** Bumped on every catalog change; a rebuild that started before a change is not cached. */
    private final AtomicLong generation = new AtomicLong();

    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "catalog-refresh");
        thread.setDaemon(true);
        return thread;
    });

    public record SimilarTo(CatalogTitleResponse seed, List<CatalogTitleResponse> items) {}

    /** Episode lists per show (they change only through migrations/admin work). */
    private final Map<Long, CachedEpisodes> episodeCache = new ConcurrentHashMap<>();

    private record CachedEpisodes(List<SeasonResponse> seasons, Instant expiresAt) {}

    private static final Duration EPISODES_TTL = Duration.ofMinutes(10);

    /** Build every level once at startup, then keep the levels in use fresh in the background. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Instant now = Instant.now();
        for (MaturityPolicy policy : MaturityPolicy.values()) {
            lastRequested.putIfAbsent(policy, now);
            scheduleRefresh(policy);
        }
        refresher.scheduleWithFixedDelay(this::refreshActive,
                REFRESH_EVERY.toSeconds(), REFRESH_EVERY.toSeconds(), TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopRefresher() {
        refresher.shutdownNow();
    }

    /**
     * Called by admin edits. Inside a transaction the snapshots are dropped only after commit, so a rebuild
     * can never cache the pre-edit catalog; then the levels in use are rebuilt in the background.
     */
    public void invalidate() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidateNow();
                }
            });
        } else {
            invalidateNow();
        }
    }

    private void invalidateNow() {
        synchronized (snapshots) {
            generation.incrementAndGet();
            snapshots.clear();
        }
        episodeCache.clear();
        Instant cutoff = Instant.now().minus(KEEP_WARM_FOR);
        lastRequested.forEach((policy, at) -> {
            if (at.isAfter(cutoff)) scheduleRefresh(policy);
        });
    }

    public record Rail(String id, String title, String kind, List<CatalogTitleResponse> items) {}
    public record HomeFeed(CatalogTitleResponse featured, List<Rail> rails) {}
    public record EpisodeResponse(String id, int episodeNumber, String title, String synopsis, Integer runtimeMinutes, String thumbnailUrl) {}
    public record SeasonResponse(int seasonNumber, String title, List<EpisodeResponse> episodes) {}

    public HomeFeed home(MaturityPolicy policy) {
        HomeFeed feed = snapshot(policy).feed();
        if (feed == null) throw new NoSuchElementException("Catalog is empty");
        return feed;
    }

    private HomeFeed buildHome(List<Content> visible) {
        if (visible.isEmpty()) return null;

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

    /** Served from the in-memory catalog snapshot; falls back to the database (and its 404 rules) otherwise. */
    public CatalogTitleResponse title(MaturityPolicy policy, String slug) {
        return cachedBySlug(policy, slug).map(this::map).orElseGet(() -> map(find(policy, slug)));
    }

    /** My List, Continue Watching, Top 10: mostly answered from the snapshot, database only for the rest. */
    public List<CatalogTitleResponse> byIds(MaturityPolicy policy, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Content> found = new java.util.HashMap<>();
        Set<Long> wanted = new java.util.HashSet<>(ids);
        for (Content c : visible(policy)) if (wanted.contains(c.getId())) found.put(c.getId(), c);
        List<Long> missing = ids.stream().filter(id -> !found.containsKey(id)).distinct().toList();
        if (!missing.isEmpty()) {
            content.findByIds(missing).stream()
                    .filter(Content::isActive)
                    .filter(c -> policy.allows(c.getContentRating()))
                    .forEach(c -> found.put(c.getId(), c));
        }
        return ids.stream().map(found::get).filter(java.util.Objects::nonNull).map(this::map).toList();
    }

    /** Titles sharing the most genres with the given one, ties broken by trending and recency. */
    public List<CatalogTitleResponse> similar(MaturityPolicy policy, String slug) {
        Content base = cachedBySlug(policy, slug).orElseGet(() -> find(policy, slug));
        return similarTo(policy, base, Set.of());
    }

    /**
     * "Because you watched": the seed title plus similar titles, minus ones the profile already started,
     * in a single call (the web app used to need two sequential round trips for this).
     */
    public SimilarTo similarById(MaturityPolicy policy, Long id, Set<Long> exclude) {
        Content base = visible(policy).stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("content " + id));
        return new SimilarTo(map(base), similarTo(policy, base, exclude));
    }

    private List<CatalogTitleResponse> similarTo(MaturityPolicy policy, Content base, Set<Long> exclude) {
        Set<Long> genres = base.getGenres().stream().map(Genre::getId).collect(Collectors.toSet());
        return visible(policy).stream()
                .filter(c -> !c.getId().equals(base.getId()))
                .map(c -> Map.entry(c, c.getGenres().stream().filter(g -> genres.contains(g.getId())).count()))
                .filter(e -> e.getValue() > 0)
                // Titles the profile already started are not removed, only ranked last: after a lot of viewing
                // (or testing) removing them could leave the row empty, and Netflix also re-surfaces watched titles.
                .sorted(Comparator.<Map.Entry<Content, Long>, Boolean>comparing(e -> exclude.contains(e.getKey().getId()))
                        .thenComparing(Comparator.<Map.Entry<Content, Long>>comparingLong(Map.Entry::getValue).reversed())
                        .thenComparing((Map.Entry<Content, Long> e) -> !e.getKey().isTrending())
                        .thenComparing((Map.Entry<Content, Long> e) -> e.getKey().getReleaseYear(), Comparator.nullsLast(Comparator.<Integer>reverseOrder())))
                .limit(12)
                .map(e -> map(e.getKey()))
                .toList();
    }

    public List<SeasonResponse> episodes(MaturityPolicy policy, String slug) {
        Content show = cachedBySlug(policy, slug).orElseGet(() -> find(policy, slug));
        if (show instanceof Movie) return List.of();
        CachedEpisodes cached = episodeCache.get(show.getId());
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.seasons();
        List<SeasonResponse> loaded = loadEpisodes(show.getId());
        episodeCache.put(show.getId(), new CachedEpisodes(loaded, Instant.now().plus(EPISODES_TTL)));
        return loaded;
    }

    private List<SeasonResponse> loadEpisodes(Long showId) {
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
        }, showId);
        return seasons.values().stream()
                .map(season -> new SeasonResponse(season.seasonNumber(), season.title(), List.copyOf(season.episodes())))
                .toList();
    }

    private java.util.Optional<Content> cachedBySlug(MaturityPolicy policy, String slug) {
        return visible(policy).stream().filter(c -> slug.equals(c.getSlug())).findFirst();
    }

    private Content find(MaturityPolicy policy, String slug) {
        Content item = content.findBySlugAndActiveTrue(slug).orElseThrow();
        if (!policy.allows(item.getContentRating())) throw new NoSuchElementException(slug);
        return item;
    }

    private List<Content> visible(MaturityPolicy policy) {
        return snapshot(policy).items();
    }

    /** The current snapshot; only a missing (or very old) one is built on the request thread. */
    private Snapshot snapshot(MaturityPolicy policy) {
        Instant now = Instant.now();
        lastRequested.put(policy, now);
        Snapshot current = snapshots.get(policy);
        if (current != null) {
            Duration age = Duration.between(current.builtAt(), now);
            if (age.compareTo(MAX_STALE) < 0) {
                if (age.compareTo(HOME_TTL) > 0) scheduleRefresh(policy);
                return current;
            }
        }
        return rebuild(policy, current == null ? "on first request" : "on request (snapshot too old)");
    }

    private void scheduleRefresh(MaturityPolicy policy) {
        if (!queued.add(policy)) return;
        try {
            refresher.execute(() -> {
                try {
                    rebuild(policy, "in background");
                } catch (RuntimeException e) {
                    log.warn("Catalog refresh for {} failed; serving the previous snapshot: {}", policy, e.toString());
                } finally {
                    queued.remove(policy);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException shuttingDown) {
            queued.remove(policy);
        }
    }

    /** Runs on the refresher thread; must never throw, or the schedule would stop. */
    private void refreshActive() {
        Instant cutoff = Instant.now().minus(KEEP_WARM_FOR);
        lastRequested.forEach((policy, at) -> {
            if (at.isAfter(cutoff)) scheduleRefresh(policy);
        });
    }

    /**
     * Loads and builds a snapshot. If the catalog changed while building, the result is not cached
     * (it may predate the change) and the build is retried once.
     */
    private Snapshot rebuild(MaturityPolicy policy, String reason) {
        Snapshot built = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            long startedGeneration = generation.get();
            built = build(policy, reason);
            synchronized (snapshots) {
                if (generation.get() == startedGeneration) {
                    snapshots.put(policy, built);
                    return built;
                }
            }
        }
        return built;
    }

    private Snapshot build(MaturityPolicy policy, String reason) {
        boolean first = !snapshots.containsKey(policy);
        long start = System.nanoTime();
        List<Long> ids = content.findActiveIdsNewestFirst(org.springframework.data.domain.PageRequest.of(0, CATALOG_SCAN_LIMIT));
        long idsDone = System.nanoTime();
        List<Content> items = List.of();
        if (!ids.isEmpty()) {
            // Duplicate-safe merge: the fetch join may return a title once per genre row.
            Map<Long, Content> byId = content.findByIds(ids).stream()
                    .collect(Collectors.toMap(Content::getId, c -> c, (kept, duplicate) -> kept));
            items = ids.stream().map(byId::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(c -> policy.allows(c.getContentRating()))
                    .toList();
        }
        long titlesDone = System.nanoTime();
        HomeFeed feed = buildHome(items);
        long end = System.nanoTime();

        long totalMs = (end - start) / 1_000_000;
        String line = "Catalog snapshot for {} rebuilt {} in {} ms (ids {} ms, titles+genres {} ms, rails {} ms, {} titles)";
        Object[] args = {policy, reason, totalMs, (idsDone - start) / 1_000_000, (titlesDone - idsDone) / 1_000_000,
                (end - titlesDone) / 1_000_000, items.size()};
        if (first || totalMs >= SLOW_REBUILD_MS) log.info(line, args);
        else log.debug(line, args);
        return new Snapshot(items, feed, Instant.now());
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
