package com.movento.recommendationservice.signals;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Plain SQL on purpose: the queries are aggregations, and the insert must be an atomic idempotent upsert. */
@Repository
public class ViewingEventStore {
    private final JdbcTemplate jdbc;

    public ViewingEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Popular(long contentId, long plays) {}

    /** @return false when this event id was already stored (a redelivery). */
    public boolean save(UUID eventId, long accountId, UUID profileId, long contentId, Instant occurredAt) {
        int inserted = jdbc.update("""
                INSERT INTO viewing_events (event_id, account_id, profile_id, content_id, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, accountId, profileId, contentId, Timestamp.from(occurredAt));
        return inserted == 1;
    }

    /** The title this profile started most recently. */
    public Optional<Long> latestContent(UUID profileId) {
        return jdbc.query("""
                SELECT content_id FROM viewing_events WHERE profile_id = ?
                ORDER BY occurred_at DESC LIMIT 1
                """, (rs, row) -> rs.getLong(1), profileId).stream().findFirst();
    }

    /** Distinct titles this profile has started, most recent first. */
    public List<Long> watchedContent(UUID profileId, int limit) {
        return jdbc.query("""
                SELECT content_id FROM viewing_events WHERE profile_id = ?
                GROUP BY content_id ORDER BY MAX(occurred_at) DESC LIMIT ?
                """, (rs, row) -> rs.getLong(1), profileId, limit);
    }

    /** Most started titles in the window, counting each profile (or account) once per title. */
    public List<Popular> popular(Duration window, int limit) {
        return jdbc.query("""
                SELECT content_id, COUNT(DISTINCT COALESCE(profile_id::text, 'account-' || account_id)) AS plays
                FROM viewing_events WHERE occurred_at >= ?
                GROUP BY content_id ORDER BY plays DESC, MAX(occurred_at) DESC LIMIT ?
                """, (rs, row) -> new Popular(rs.getLong(1), rs.getLong(2)), Timestamp.from(Instant.now().minus(window)), limit);
    }
}
