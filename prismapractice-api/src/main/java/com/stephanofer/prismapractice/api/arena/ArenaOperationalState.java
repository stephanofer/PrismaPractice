package com.stephanofer.prismapractice.api.arena;

import java.time.Instant;
import java.util.Objects;

public record ArenaOperationalState(
        ArenaId arenaId,
        ArenaOperationalStatus status,
        String reservationId,
        Instant updatedAt,
        String lastFailureReason
) {

    public ArenaOperationalState {
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(lastFailureReason, "lastFailureReason");
    }

    public static ArenaOperationalState available(ArenaId arenaId, Instant now) {
        return new ArenaOperationalState(arenaId, ArenaOperationalStatus.AVAILABLE, "", now, "");
    }
}
