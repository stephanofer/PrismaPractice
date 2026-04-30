package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.arena.ArenaReservation;
import com.stephanofer.prismapractice.api.arena.ArenaType;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;

import java.util.Objects;

public record MatchCreationRequest(
        MatchmakingProposal proposal,
        ArenaReservation reservation,
        ArenaType arenaType,
        PlayerType playerType,
        RuntimeType runtimeType,
        EffectiveMatchConfig effectiveConfig
) {

    public MatchCreationRequest {
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(reservation, "reservation");
        Objects.requireNonNull(arenaType, "arenaType");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(effectiveConfig, "effectiveConfig");
    }
}
