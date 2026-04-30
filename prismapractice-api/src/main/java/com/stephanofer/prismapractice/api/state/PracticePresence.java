package com.stephanofer.prismapractice.api.state;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;

import java.time.Instant;
import java.util.Objects;

public record PracticePresence(
        PlayerId playerId,
        boolean online,
        RuntimeType runtimeType,
        String serverId,
        Instant updatedAt
) {

    public PracticePresence {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (online && serverId.isBlank()) {
            throw new IllegalArgumentException("serverId must not be blank while online");
        }
    }

    public static PracticePresence online(PlayerId playerId, RuntimeType runtimeType, String serverId, Instant now) {
        return new PracticePresence(playerId, true, runtimeType, serverId, now);
    }

    public static PracticePresence offline(PlayerId playerId, Instant now) {
        return new PracticePresence(playerId, false, RuntimeType.UNKNOWN, "", now);
    }
}
