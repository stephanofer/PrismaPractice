package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RedisPlayerStateRepository implements PlayerStateRepository {

    private final RedisStorage redisStorage;

    public RedisPlayerStateRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<PlayerState> find(PlayerId playerId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().playerState(playerId.toString()));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 3) {
            delete(playerId);
            return Optional.empty();
        }
        return Optional.of(new PlayerState(
                playerId,
                PlayerStatus.valueOf(parts[0]),
                PlayerSubStatus.valueOf(parts[1]),
                Instant.ofEpochMilli(Long.parseLong(parts[2]))
        ));
    }

    @Override
    public PlayerState save(PlayerState state) {
        redisStorage.syncCommands().set(
                redisStorage.keyspace().playerState(state.playerId().toString()),
                state.status().name() + '|' + state.subStatus().name() + '|' + state.updatedAt().toEpochMilli()
        );
        return state;
    }

    @Override
    public void delete(PlayerId playerId) {
        redisStorage.syncCommands().del(redisStorage.keyspace().playerState(playerId.toString()));
    }
}
