package com.stephanofer.prismapractice.api.queue;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;

import java.time.Instant;
import java.util.Objects;

public record QueueEntry(
        PlayerId playerId,
        QueueId queueId,
        Instant joinedAt,
        MatchmakingProfile matchmakingProfile,
        QueueType queueType,
        PlayerType playerType,
        String sourceServerId
) {

    public QueueEntry {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(joinedAt, "joinedAt");
        Objects.requireNonNull(matchmakingProfile, "matchmakingProfile");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(sourceServerId, "sourceServerId");
    }
}
