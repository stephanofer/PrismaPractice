package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.leaderboard.*;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RedisLeaderboardProjectionRepository implements LeaderboardProjectionRepository {

    private final RedisStorage redisStorage;

    public RedisLeaderboardProjectionRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public void upsert(LeaderboardScope scope, LeaderboardEntry entry) {
        String key = key(scope);
        redisStorage.syncCommands().zadd(key, entry.score(), entry.playerId().toString());
        redisStorage.syncCommands().set(entryKey(scope, entry.playerId()), serialize(entry));
    }

    @Override
    public void remove(LeaderboardScope scope, PlayerId playerId) {
        redisStorage.syncCommands().zrem(key(scope), playerId.toString());
        redisStorage.syncCommands().del(entryKey(scope, playerId));
    }

    @Override
    public List<LeaderboardEntry> top(LeaderboardQuery query) {
        String key = key(query.scope());
        long start = query.offset();
        long stop = query.offset() + query.limit() - 1L;
        List<String> playerIds = redisStorage.syncCommands().zrevrange(key, start, stop);
        List<LeaderboardEntry> entries = new ArrayList<>();
        int position = query.offset() + 1;
        for (String rawPlayerId : playerIds) {
            String rawEntry = redisStorage.syncCommands().get(entryKey(query.scope(), PlayerId.fromString(rawPlayerId)));
            if (rawEntry == null || rawEntry.isBlank()) {
                redisStorage.syncCommands().zrem(key, rawPlayerId);
                continue;
            }
            LeaderboardEntry parsed = parse(rawEntry, position++);
            entries.add(parsed);
        }
        return List.copyOf(entries);
    }

    @Override
    public void clear(LeaderboardScope scope) {
        redisStorage.syncCommands().del(key(scope));
    }

    private String key(LeaderboardScope scope) {
        return switch (scope.type()) {
            case GLOBAL -> redisStorage.keyspace().leaderboardGlobal();
            case MODE -> redisStorage.keyspace().leaderboardMode(scope.modeId().toString());
            case SEASON_GLOBAL -> redisStorage.keyspace().leaderboardSeasonGlobal(scope.seasonId());
            case SEASON_MODE -> redisStorage.keyspace().leaderboardSeasonMode(scope.seasonId(), scope.modeId().toString());
        };
    }

    private String entryKey(LeaderboardScope scope, PlayerId playerId) {
        return redisStorage.keyspace().leaderboardEntry(scopeKey(scope), playerId.toString());
    }

    private String scopeKey(LeaderboardScope scope) {
        return switch (scope.type()) {
            case GLOBAL -> "global";
            case MODE -> "mode-" + scope.modeId();
            case SEASON_GLOBAL -> "season-" + scope.seasonId() + "-global";
            case SEASON_MODE -> "season-" + scope.seasonId() + "-mode-" + scope.modeId();
        };
    }

    private String serialize(LeaderboardEntry entry) {
        return entry.playerId() + "|" + escape(entry.playerName()) + "|" + entry.score() + "|" + escape(entry.rankKey());
    }

    private LeaderboardEntry parse(String raw, int position) {
        String[] parts = raw.split("\\|", -1);
        return new LeaderboardEntry(PlayerId.fromString(parts[0]), unescape(parts[1]), Integer.parseInt(parts[2]), unescape(parts[3]), position);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private String unescape(String value) {
        return value.replace("\\p", "|").replace("\\\\", "\\");
    }
}
