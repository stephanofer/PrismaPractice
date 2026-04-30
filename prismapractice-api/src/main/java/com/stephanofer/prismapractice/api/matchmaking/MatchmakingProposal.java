package com.stephanofer.prismapractice.api.matchmaking;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;

import java.util.Objects;

public record MatchmakingProposal(
        QueueId queueId,
        PlayerId leftPlayerId,
        PlayerId rightPlayerId,
        RegionId selectedRegion,
        int qualityScore,
        MatchmakingSearchWindow searchWindow,
        String reasoningSummary,
        String leftLockToken,
        String rightLockToken
) {

    public MatchmakingProposal {
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(leftPlayerId, "leftPlayerId");
        Objects.requireNonNull(rightPlayerId, "rightPlayerId");
        Objects.requireNonNull(selectedRegion, "selectedRegion");
        Objects.requireNonNull(searchWindow, "searchWindow");
        Objects.requireNonNull(reasoningSummary, "reasoningSummary");
        Objects.requireNonNull(leftLockToken, "leftLockToken");
        Objects.requireNonNull(rightLockToken, "rightLockToken");
        if (leftPlayerId.equals(rightPlayerId)) {
            throw new IllegalArgumentException("Matchmaking proposal must contain two different players");
        }
    }
}
