package com.stephanofer.prismapractice.api.history;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Objects;

public record MatchHistoryEvent(
        long timestampOffsetMs,
        MatchHistoryEventType eventType,
        PlayerId actorPlayerId,
        PlayerId targetPlayerId,
        String detailsJson
) {

    public MatchHistoryEvent {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(detailsJson, "detailsJson");
        if (timestampOffsetMs < 0) {
            throw new IllegalArgumentException("timestampOffsetMs must be >= 0");
        }
    }
}
