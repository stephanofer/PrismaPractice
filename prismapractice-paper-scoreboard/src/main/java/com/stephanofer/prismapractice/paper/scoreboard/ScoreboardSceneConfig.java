package com.stephanofer.prismapractice.paper.scoreboard;

import java.util.List;
import java.util.Objects;

public record ScoreboardSceneConfig(
        String key,
        int priority,
        int refreshIntervalTicks,
        ScoreboardSceneConditions conditions,
        String title,
        List<String> lines
) {

    public ScoreboardSceneConfig {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (refreshIntervalTicks < 1) {
            throw new IllegalArgumentException("refreshIntervalTicks must be >= 1");
        }
        if (lines.size() > 15) {
            throw new IllegalArgumentException("scoreboard scenes support up to 15 lines");
        }
    }
}
