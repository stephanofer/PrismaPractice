package com.stephanofer.prismapractice.api.history;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.match.SeriesFormat;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.QueueType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MatchHistorySummary(
        MatchId matchId,
        ModeId modeId,
        QueueType queueType,
        SeriesFormat seriesFormat,
        ArenaId arenaId,
        RegionId regionId,
        String runtimeServerId,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds,
        PlayerId winnerPlayerId,
        List<MatchHistoryPlayerSnapshot> players,
        List<MatchHistoryStatEntry> stats,
        List<MatchHistoryEvent> events
) {

    public MatchHistorySummary {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(seriesFormat, "seriesFormat");
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(runtimeServerId, "runtimeServerId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        Objects.requireNonNull(winnerPlayerId, "winnerPlayerId");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(events, "events");
        players = List.copyOf(players);
        stats = List.copyOf(stats);
        events = List.copyOf(events);
        if (runtimeServerId.isBlank()) {
            throw new IllegalArgumentException("runtimeServerId must not be blank");
        }
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds must be >= 0");
        }
    }
}
