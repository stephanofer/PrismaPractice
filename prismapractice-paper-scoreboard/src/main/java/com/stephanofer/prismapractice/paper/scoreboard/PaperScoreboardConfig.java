package com.stephanofer.prismapractice.paper.scoreboard;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PaperScoreboardConfig(
        PaperScoreboardSettings settings,
        List<ScoreboardSceneConfig> scenes,
        Map<String, ScoreboardSceneConfig> scenesByKey
) {

    public PaperScoreboardConfig {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(scenes, "scenes");
        Objects.requireNonNull(scenesByKey, "scenesByKey");
        scenes = List.copyOf(scenes);
        scenesByKey = Map.copyOf(scenesByKey);
    }
}
