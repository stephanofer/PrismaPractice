package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.queue.QueueType;

import java.time.Instant;
import java.util.Objects;

public record ScoreboardQueueView(
        String queueId,
        String displayName,
        QueueType queueType,
        PlayerType playerType,
        Instant joinedAt,
        int playerCount
) {

    public ScoreboardQueueView {
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(joinedAt, "joinedAt");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (playerCount < 0) {
            throw new IllegalArgumentException("playerCount must be >= 0");
        }
    }
}
