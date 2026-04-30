package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardUiStateService {

    private final Map<PlayerId, ScoreboardUiFocus> focuses = new ConcurrentHashMap<>();

    public ScoreboardUiFocus focus(PlayerId playerId) {
        return focuses.getOrDefault(playerId, ScoreboardUiFocus.NONE);
    }

    public void setFocus(PlayerId playerId, ScoreboardUiFocus focus) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(focus, "focus");
        if (focus == ScoreboardUiFocus.NONE) {
            focuses.remove(playerId);
            return;
        }
        focuses.put(playerId, focus);
    }

    public void clear(PlayerId playerId) {
        focuses.remove(playerId);
    }
}
