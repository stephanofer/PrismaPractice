package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.ActiveMatch;
import com.stephanofer.prismapractice.api.match.ActiveMatchRepository;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.match.MatchStatus;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RedisActiveMatchRepository implements ActiveMatchRepository {

    private final RedisStorage redisStorage;

    public RedisActiveMatchRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<ActiveMatch> findByMatchId(MatchId matchId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().activeMatch(matchId.toString()));
        return Optional.ofNullable(raw == null || raw.isBlank() ? null : parse(raw));
    }

    @Override
    public Optional<ActiveMatch> findByPlayerId(PlayerId playerId) {
        String rawMatchId = redisStorage.syncCommands().get(redisStorage.keyspace().playerActiveMatch(playerId.toString()));
        if (rawMatchId == null || rawMatchId.isBlank()) return Optional.empty();
        return findByMatchId(MatchId.fromString(rawMatchId));
    }

    @Override
    public ActiveMatch save(ActiveMatch activeMatch) {
        String raw = serialize(activeMatch);
        redisStorage.syncCommands().set(redisStorage.keyspace().activeMatch(activeMatch.matchId().toString()), raw);
        redisStorage.syncCommands().set(redisStorage.keyspace().playerActiveMatch(activeMatch.leftPlayerId().toString()), activeMatch.matchId().toString());
        redisStorage.syncCommands().set(redisStorage.keyspace().playerActiveMatch(activeMatch.rightPlayerId().toString()), activeMatch.matchId().toString());
        return activeMatch;
    }

    @Override
    public void remove(MatchId matchId, PlayerId leftPlayerId, PlayerId rightPlayerId) {
        redisStorage.syncCommands().del(
                redisStorage.keyspace().activeMatch(matchId.toString()),
                redisStorage.keyspace().playerActiveMatch(leftPlayerId.toString()),
                redisStorage.keyspace().playerActiveMatch(rightPlayerId.toString())
        );
    }

    private ActiveMatch parse(String raw) {
        String[] parts = raw.split("\\|", -1);
        return new ActiveMatch(MatchId.fromString(parts[0]), new ArenaId(parts[1]), PlayerId.fromString(parts[2]), PlayerId.fromString(parts[3]), MatchStatus.valueOf(parts[4]), parts[5], Instant.ofEpochMilli(Long.parseLong(parts[6])));
    }

    private String serialize(ActiveMatch activeMatch) {
        return activeMatch.matchId() + "|" + activeMatch.arenaId() + "|" + activeMatch.leftPlayerId() + "|" + activeMatch.rightPlayerId() + "|" + activeMatch.status().name() + "|" + activeMatch.runtimeServerId() + "|" + activeMatch.updatedAt().toEpochMilli();
    }
}
