package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;

import java.time.Instant;
import java.util.Objects;

public record RatingChange(
        MatchId matchId,
        PlayerId playerId,
        ModeId modeId,
        int beforeSr,
        int afterSr,
        int delta,
        String beforeRankKey,
        String afterRankKey,
        int globalBefore,
        int globalAfter,
        Instant appliedAt
) {

    public RatingChange {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(beforeRankKey, "beforeRankKey");
        Objects.requireNonNull(afterRankKey, "afterRankKey");
        Objects.requireNonNull(appliedAt, "appliedAt");
    }
}
