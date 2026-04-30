package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PracticePresence;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RedisPlayerPresenceRepository implements PlayerPresenceRepository {

    private final RedisStorage redisStorage;

    public RedisPlayerPresenceRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<PracticePresence> find(PlayerId playerId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().playerPresence(playerId.toString()));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 4) {
            delete(playerId);
            return Optional.empty();
        }
        return Optional.of(new PracticePresence(
                playerId,
                Boolean.parseBoolean(parts[0]),
                RuntimeType.valueOf(parts[1]),
                parts[2],
                Instant.ofEpochMilli(Long.parseLong(parts[3]))
        ));
    }

    @Override
    public PracticePresence save(PracticePresence presence) {
        String key = redisStorage.keyspace().playerPresence(presence.playerId().toString());
        String value = presence.online() + "|" + presence.runtimeType().name() + "|" + presence.serverId() + "|" + presence.updatedAt().toEpochMilli();
        if (presence.online()) {
            redisStorage.syncCommands().psetex(key, redisStorage.ttlPolicies().playerPresence().toMillis(), value);
        } else {
            redisStorage.syncCommands().set(key, value);
        }
        return presence;
    }

    @Override
    public void delete(PlayerId playerId) {
        redisStorage.syncCommands().del(redisStorage.keyspace().playerPresence(playerId.toString()));
    }
}
