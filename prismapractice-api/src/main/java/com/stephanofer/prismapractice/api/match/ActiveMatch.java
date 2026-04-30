package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.PlayerId;

import java.time.Instant;
import java.util.Objects;

public record ActiveMatch(
        MatchId matchId,
        ArenaId arenaId,
        PlayerId leftPlayerId,
        PlayerId rightPlayerId,
        MatchStatus status,
        String runtimeServerId,
        Instant updatedAt
) {

    public ActiveMatch {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(leftPlayerId, "leftPlayerId");
        Objects.requireNonNull(rightPlayerId, "rightPlayerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(runtimeServerId, "runtimeServerId");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
