package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.queue.PlayerPartyIndexRepository;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.util.Objects;

public final class RedisPlayerPartyIndexRepository implements PlayerPartyIndexRepository {

    private final RedisStorage redisStorage;

    public RedisPlayerPartyIndexRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public boolean isInParty(PlayerId playerId) {
        return redisStorage.syncCommands().exists(redisStorage.keyspace().playerParty(playerId.toString())) > 0;
    }
}
