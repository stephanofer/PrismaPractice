package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaType;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MatchSession(
        MatchId matchId,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        MatchStatus status,
        PlayerType playerType,
        List<MatchParticipant> participants,
        ArenaId arenaId,
        ArenaType arenaType,
        RegionId regionId,
        RuntimeType runtimeType,
        String runtimeServerId,
        EffectiveMatchConfig effectiveConfig,
        MatchScore score,
        PlayerId winnerPlayerId,
        PlayerId loserPlayerId,
        MatchResultType resultType,
        String cancelReason,
        String failureReason,
        boolean recoverable
) {

    public MatchSession {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(participants, "participants");
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(arenaType, "arenaType");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(runtimeServerId, "runtimeServerId");
        Objects.requireNonNull(effectiveConfig, "effectiveConfig");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(cancelReason, "cancelReason");
        Objects.requireNonNull(failureReason, "failureReason");
        participants = List.copyOf(participants);
    }
}
