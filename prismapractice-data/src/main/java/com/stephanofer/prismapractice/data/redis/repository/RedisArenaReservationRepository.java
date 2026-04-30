package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaReservation;
import com.stephanofer.prismapractice.api.arena.ArenaReservationRepository;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RedisArenaReservationRepository implements ArenaReservationRepository {

    private final RedisStorage redisStorage;

    public RedisArenaReservationRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<ArenaReservation> findByArenaId(ArenaId arenaId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().arenaState(arenaId.toString()) + ":reservation");
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 9) {
            return Optional.empty();
        }
        return Optional.of(new ArenaReservation(
                parts[0],
                new ArenaId(parts[1]),
                new QueueId(parts[2]),
                PlayerId.fromString(parts[3]),
                PlayerId.fromString(parts[4]),
                new RegionId(parts[5]),
                RuntimeType.valueOf(parts[6]),
                Instant.ofEpochMilli(Long.parseLong(parts[7])),
                Instant.ofEpochMilli(Long.parseLong(parts[8]))
        ));
    }

    @Override
    public ArenaReservation save(ArenaReservation reservation) {
        redisStorage.syncCommands().psetex(
                redisStorage.keyspace().arenaState(reservation.arenaId().toString()) + ":reservation",
                Math.max(1000L, reservation.expiresAt().toEpochMilli() - reservation.reservedAt().toEpochMilli()),
                serialize(reservation)
        );
        return reservation;
    }

    @Override
    public boolean remove(ArenaId arenaId, String reservationId) {
        Optional<ArenaReservation> current = findByArenaId(arenaId);
        if (current.isEmpty()) {
            return false;
        }
        if (!reservationId.isBlank() && !current.get().reservationId().equals(reservationId)) {
            return false;
        }
        Long removed = redisStorage.syncCommands().del(redisStorage.keyspace().arenaState(arenaId.toString()) + ":reservation");
        return removed != null && removed > 0;
    }

    private String serialize(ArenaReservation reservation) {
        return reservation.reservationId() + '|'
                + reservation.arenaId() + '|'
                + reservation.queueId() + '|'
                + reservation.leftPlayerId() + '|'
                + reservation.rightPlayerId() + '|'
                + reservation.regionId().value() + '|'
                + reservation.runtimeType().name() + '|'
                + reservation.reservedAt().toEpochMilli() + '|'
                + reservation.expiresAt().toEpochMilli();
    }
}
