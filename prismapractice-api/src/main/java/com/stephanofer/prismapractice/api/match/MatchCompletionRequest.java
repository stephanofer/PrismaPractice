package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Objects;

public record MatchCompletionRequest(PlayerId winnerPlayerId, PlayerId loserPlayerId, MatchResultType resultType) {

    public MatchCompletionRequest {
        Objects.requireNonNull(winnerPlayerId, "winnerPlayerId");
        Objects.requireNonNull(loserPlayerId, "loserPlayerId");
        Objects.requireNonNull(resultType, "resultType");
    }
}
