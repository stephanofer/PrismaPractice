package com.stephanofer.prismapractice.api.match;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.state.PlayerStatus;

import java.util.Objects;

public record MatchParticipant(PlayerId playerId, MatchSide side, PlayerStatus startingState, boolean connectedAtStart) {

    public MatchParticipant {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(startingState, "startingState");
    }
}
