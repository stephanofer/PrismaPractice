package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.time.Instant;
import java.util.Objects;

public record GlobalRatingSnapshot(
        PlayerId playerId,
        int currentGlobalRating,
        String currentGlobalRankKey,
        int peakGlobalRating,
        String peakGlobalRankKey,
        String seasonId,
        Instant updatedAt
) {

    public GlobalRatingSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(currentGlobalRankKey, "currentGlobalRankKey");
        Objects.requireNonNull(peakGlobalRankKey, "peakGlobalRankKey");
        Objects.requireNonNull(seasonId, "seasonId");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
