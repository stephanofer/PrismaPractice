package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalState;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalStateRepository;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalStatus;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RedisArenaOperationalStateRepository implements ArenaOperationalStateRepository {

    private final RedisStorage redisStorage;

    public RedisArenaOperationalStateRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<ArenaOperationalState> find(ArenaId arenaId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().arenaState(arenaId.toString()));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 4) {
            return Optional.empty();
        }
        return Optional.of(new ArenaOperationalState(
                arenaId,
                ArenaOperationalStatus.valueOf(parts[0]),
                parts[1],
                Instant.ofEpochMilli(Long.parseLong(parts[2])),
                parts[3]
        ));
    }

    @Override
    public ArenaOperationalState save(ArenaOperationalState state) {
        redisStorage.syncCommands().set(
                redisStorage.keyspace().arenaState(state.arenaId().toString()),
                state.status().name() + '|' + state.reservationId() + '|' + state.updatedAt().toEpochMilli() + '|' + state.lastFailureReason()
        );
        return state;
    }
}
