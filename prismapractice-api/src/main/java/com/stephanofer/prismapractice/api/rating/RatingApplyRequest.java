package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.queue.QueueType;

import java.util.Objects;

public record RatingApplyRequest(
        MatchId matchId,
        ModeId modeId,
        QueueType queueType,
        PlayerId winnerPlayerId,
        PlayerId loserPlayerId,
        boolean affectsSr,
        boolean affectsVisibleRank,
        boolean affectsGlobalRating,
        String seasonId
) {

    public RatingApplyRequest {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(queueType, "queueType");
        Objects.requireNonNull(winnerPlayerId, "winnerPlayerId");
        Objects.requireNonNull(loserPlayerId, "loserPlayerId");
        Objects.requireNonNull(seasonId, "seasonId");
    }
}
