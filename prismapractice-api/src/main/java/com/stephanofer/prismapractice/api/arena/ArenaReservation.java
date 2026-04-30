package com.stephanofer.prismapractice.api.arena;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;

import java.time.Instant;
import java.util.Objects;

public record ArenaReservation(
        String reservationId,
        ArenaId arenaId,
        QueueId queueId,
        PlayerId leftPlayerId,
        PlayerId rightPlayerId,
        RegionId regionId,
        RuntimeType runtimeType,
        Instant reservedAt,
        Instant expiresAt
) {

    public ArenaReservation {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(leftPlayerId, "leftPlayerId");
        Objects.requireNonNull(rightPlayerId, "rightPlayerId");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(reservedAt, "reservedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
    }
}
