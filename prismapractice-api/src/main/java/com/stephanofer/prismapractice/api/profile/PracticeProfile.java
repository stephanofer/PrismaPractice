package com.stephanofer.prismapractice.api.profile;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record PracticeProfile(
        PlayerId playerId,
        String currentName,
        String normalizedName,
        Instant firstSeenAt,
        Instant lastSeenAt,
        ProfileVisibility visibility,
        int currentGlobalRating,
        String currentGlobalRankKey
) {

    public PracticeProfile {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(currentName, "currentName");
        Objects.requireNonNull(normalizedName, "normalizedName");
        Objects.requireNonNull(firstSeenAt, "firstSeenAt");
        Objects.requireNonNull(lastSeenAt, "lastSeenAt");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(currentGlobalRankKey, "currentGlobalRankKey");
        if (currentName.isBlank()) {
            throw new IllegalArgumentException("currentName must not be blank");
        }
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("normalizedName must not be blank");
        }
    }

    public static PracticeProfile bootstrap(PlayerId playerId, String currentName, Instant now) {
        Objects.requireNonNull(now, "now");
        return new PracticeProfile(
                playerId,
                requireName(currentName),
                normalizeName(currentName),
                now,
                now,
                ProfileVisibility.PUBLIC,
                0,
                "unranked"
        );
    }

    public PracticeProfile touch(String currentName, Instant now) {
        Objects.requireNonNull(now, "now");
        String safeName = requireName(currentName);
        return new PracticeProfile(
                playerId,
                safeName,
                normalizeName(safeName),
                firstSeenAt,
                now,
                visibility,
                currentGlobalRating,
                currentGlobalRankKey
        );
    }

    public static String normalizeName(String currentName) {
        return requireName(currentName).trim().toLowerCase(Locale.ROOT);
    }

    private static String requireName(String currentName) {
        Objects.requireNonNull(currentName, "currentName");
        if (currentName.isBlank()) {
            throw new IllegalArgumentException("currentName must not be blank");
        }
        return currentName;
    }
}
