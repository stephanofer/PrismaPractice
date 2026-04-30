package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueType;

import java.util.List;
import java.util.Objects;

public record EffectiveMatchConfig(
        ModeId modeId,
        QueueId queueId,
        QueueType queueType,
        MatchmakingProfile matchmakingProfile,
        int effectiveRoundsToWin,
        int effectivePreFightDelaySeconds,
        int effectiveMaxDurationSeconds,
        String effectiveKitReference,
        List<String> effectiveRules,
        boolean spectatorAllowed,
        SeriesFormat seriesFormat
) {

    public EffectiveMatchConfig {
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(matchmakingProfile, "matchmakingProfile");
        Objects.requireNonNull(effectiveKitReference, "effectiveKitReference");
        Objects.requireNonNull(effectiveRules, "effectiveRules");
        Objects.requireNonNull(seriesFormat, "seriesFormat");
        effectiveRules = List.copyOf(effectiveRules);
        if (effectiveRoundsToWin <= 0 || effectivePreFightDelaySeconds < 0 || effectiveMaxDurationSeconds <= 0) {
            throw new IllegalArgumentException("Invalid effective match config values");
        }
    }
}
