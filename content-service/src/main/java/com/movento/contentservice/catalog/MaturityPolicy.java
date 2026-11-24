package com.movento.contentservice.catalog;

import java.util.Locale;
import java.util.Set;

/**
 * Maps a viewer profile's maturity level (from the X-Profile-Maturity header the gateway derives
 * from the profile-scoped JWT) to the content ratings that profile may see.
 * Unknown ratings are treated as adult-only so new content never leaks into kids profiles.
 */
public enum MaturityPolicy {
    KIDS(Set.of("G", "PG", "TV-Y", "TV-Y7", "TV-G", "TV-PG")),
    TEEN(Set.of("G", "PG", "PG-13", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14")),
    ADULT(null);

    private final Set<String> allowedRatings;

    MaturityPolicy(Set<String> allowedRatings) {
        this.allowedRatings = allowedRatings;
    }

    public boolean allows(String rating) {
        if (allowedRatings == null) return true;
        return rating != null && allowedRatings.contains(rating.trim().toUpperCase(Locale.ROOT));
    }

    public static MaturityPolicy fromHeader(String header) {
        if (header == null || header.isBlank()) return ADULT;
        try {
            return valueOf(header.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return KIDS; // fail closed
        }
    }
}
