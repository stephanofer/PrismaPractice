package com.stephanofer.prismapractice.api.matchmaking;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MatchmakingSnapshot(
        PlayerId playerId,
        QueueId queueId,
        Instant joinedAt,
        PlayerStatus stateAtCapture,
        int skillValue,
        PingRangePreference pingRangePreference,
        List<RegionPing> regionPings,
        String sourceServerId,
        Instant capturedAt
) {

    public MatchmakingSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(joinedAt, "joinedAt");
        Objects.requireNonNull(stateAtCapture, "stateAtCapture");
        Objects.requireNonNull(pingRangePreference, "pingRangePreference");
        Objects.requireNonNull(regionPings, "regionPings");
        Objects.requireNonNull(sourceServerId, "sourceServerId");
        Objects.requireNonNull(capturedAt, "capturedAt");
        regionPings = List.copyOf(regionPings);
    }
}
