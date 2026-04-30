package com.stephanofer.prismapractice.api.state;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.time.Instant;
import java.util.Objects;

public record PlayerState(PlayerId playerId, PlayerStatus status, PlayerSubStatus subStatus, Instant updatedAt) {

    public PlayerState {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(subStatus, "subStatus");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static PlayerState hub(PlayerId playerId, Instant now) {
        return new PlayerState(playerId, PlayerStatus.HUB, PlayerSubStatus.NONE, now);
    }

    public static PlayerState offline(PlayerId playerId, Instant now) {
        return new PlayerState(playerId, PlayerStatus.OFFLINE, PlayerSubStatus.NONE, now);
    }

    public PlayerState withStatus(PlayerStatus status, PlayerSubStatus subStatus, Instant now) {
        return new PlayerState(playerId, status, subStatus, now);
    }
}
