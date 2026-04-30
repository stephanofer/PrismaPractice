package com.stephanofer.prismapractice.api.matchmaking;

import java.time.Duration;
import java.util.Objects;

public record MatchmakingSearchWindow(
        String key,
        Duration minimumQueueTime,
        int maxSkillDelta,
        int minimumQualityScore
) {

    public MatchmakingSearchWindow {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(minimumQueueTime, "minimumQueueTime");
        if (maxSkillDelta < 0) {
            throw new IllegalArgumentException("maxSkillDelta must be >= 0");
        }
    }
}
