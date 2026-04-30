package com.stephanofer.prismapractice.api.arena;

import java.util.Optional;

public interface ArenaReservationRepository {

    Optional<ArenaReservation> findByArenaId(ArenaId arenaId);

    ArenaReservation save(ArenaReservation reservation);

    boolean remove(ArenaId arenaId, String reservationId);
}
