package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;

import java.time.Instant;
import java.util.Objects;

public record ModeRating(
        PlayerId playerId,
        ModeId modeId,
        int currentSr,
        String currentRankKey,
        int peakSr,
        String peakRankKey,
        boolean placementsCompleted,
        int placementsPlayed,
        String seasonId,
        Instant updatedAt,
        int wins,
        int losses
) {

    public ModeRating {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(currentRankKey, "currentRankKey");
        Objects.requireNonNull(peakRankKey, "peakRankKey");
        Objects.requireNonNull(seasonId, "seasonId");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
