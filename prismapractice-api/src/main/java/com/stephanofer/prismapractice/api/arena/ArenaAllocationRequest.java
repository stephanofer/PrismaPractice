package com.stephanofer.prismapractice.api.arena;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;

import java.util.Objects;

public record ArenaAllocationRequest(
        MatchmakingProposal proposal,
        ArenaType arenaType,
        ModeId modeId,
        PlayerType playerType,
        RuntimeType targetRuntime,
        RegionId requiredRegion
) {

    public ArenaAllocationRequest {
        Objects.requireNonNull(proposal, "proposal");
        Objects.requireNonNull(arenaType, "arenaType");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(playerType, "playerType");
        Objects.requireNonNull(targetRuntime, "targetRuntime");
        Objects.requireNonNull(requiredRegion, "requiredRegion");
    }
}
