package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import io.lettuce.core.SetArgs;
import io.lettuce.core.ScriptOutputType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RedisPlayerOperationLockRepository implements PlayerOperationLockRepository {

    private static final String COMPARE_DELETE_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final RedisStorage redisStorage;

    public RedisPlayerOperationLockRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<String> acquireTransitionLock(PlayerId playerId) {
        return acquire(redisStorage.keyspace().transitionLock(playerId.toString()), redisStorage.ttlPolicies().transitionLock().toMillis());
    }

    @Override
    public boolean releaseTransitionLock(PlayerId playerId, String token) {
        return release(redisStorage.keyspace().transitionLock(playerId.toString()), token);
    }

    @Override
    public Optional<String> acquireMatchmakingLock(QueueId queueId, PlayerId playerId) {
        return acquire(redisStorage.keyspace().matchmakingLock(queueId.toString(), playerId.toString()), redisStorage.ttlPolicies().matchmakingLock().toMillis());
    }

    @Override
    public boolean releaseMatchmakingLock(QueueId queueId, PlayerId playerId, String token) {
        return release(redisStorage.keyspace().matchmakingLock(queueId.toString(), playerId.toString()), token);
    }

    @Override
    public Optional<String> acquireArenaLock(ArenaId arenaId) {
        return acquire(redisStorage.keyspace().arenaLock(arenaId.toString()), redisStorage.ttlPolicies().arenaLock().toMillis());
    }

    @Override
    public boolean releaseArenaLock(ArenaId arenaId, String token) {
        return release(redisStorage.keyspace().arenaLock(arenaId.toString()), token);
    }

    private Optional<String> acquire(String key, long ttlMillis) {
        String token = UUID.randomUUID().toString();
        String result = redisStorage.syncCommands().set(key, token, SetArgs.Builder.nx().px(ttlMillis));
        return "OK".equals(result) ? Optional.of(token) : Optional.empty();
    }

    private boolean release(String key, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long result = redisStorage.syncCommands().eval(COMPARE_DELETE_LUA, ScriptOutputType.INTEGER, new String[]{key}, token);
        return result != null && result > 0;
    }
}
