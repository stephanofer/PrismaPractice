package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RedisQueueEntryRepository implements QueueEntryRepository {

    private final RedisStorage redisStorage;

    public RedisQueueEntryRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<QueueEntry> findByPlayerId(PlayerId playerId) {
        String queueId = redisStorage.syncCommands().get(redisStorage.keyspace().playerQueue(playerId.toString()));
        if (queueId == null || queueId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(readEntry(new QueueId(queueId), playerId));
    }

    @Override
    public List<QueueEntry> findByQueueId(QueueId queueId) {
        List<String> playerIds = redisStorage.syncCommands().zrange(redisStorage.keyspace().queueEntries(queueId.toString()), 0, -1);
        List<QueueEntry> entries = new ArrayList<>();
        for (String rawPlayerId : playerIds) {
            QueueEntry entry = readEntry(queueId, PlayerId.fromString(rawPlayerId));
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    @Override
    public QueueEntry save(QueueEntry entry) {
        String queueKey = redisStorage.keyspace().queueEntries(entry.queueId().toString());
        String playerQueueKey = redisStorage.keyspace().playerQueue(entry.playerId().toString());
        String searchKey = redisStorage.keyspace().queueSearch(entry.queueId().toString(), entry.playerId().toString());
        redisStorage.syncCommands().multi();
        redisStorage.syncCommands().set(playerQueueKey, entry.queueId().toString());
        redisStorage.syncCommands().zadd(queueKey, entry.joinedAt().toEpochMilli(), entry.playerId().toString());
        redisStorage.syncCommands().set(searchKey, serialize(entry));
        redisStorage.syncCommands().exec();
        return entry;
    }

    @Override
    public boolean removeByPlayerId(PlayerId playerId) {
        Optional<QueueEntry> existing = findByPlayerId(playerId);
        return existing.isPresent() && remove(existing.get().queueId(), playerId);
    }

    @Override
    public boolean remove(QueueId queueId, PlayerId playerId) {
        String queueKey = redisStorage.keyspace().queueEntries(queueId.toString());
        String playerQueueKey = redisStorage.keyspace().playerQueue(playerId.toString());
        String searchKey = redisStorage.keyspace().queueSearch(queueId.toString(), playerId.toString());
        Long removed = redisStorage.syncCommands().zrem(queueKey, playerId.toString());
        redisStorage.syncCommands().del(playerQueueKey, searchKey);
        return removed != null && removed > 0;
    }

    private QueueEntry readEntry(QueueId queueId, PlayerId playerId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().queueSearch(queueId.toString(), playerId.toString()));
        if (raw == null || raw.isBlank()) {
            redisStorage.syncCommands().del(redisStorage.keyspace().playerQueue(playerId.toString()));
            redisStorage.syncCommands().zrem(redisStorage.keyspace().queueEntries(queueId.toString()), playerId.toString());
            return null;
        }

        String[] parts = raw.split("\\|", -1);
        if (parts.length != 7) {
            remove(queueId, playerId);
            return null;
        }

        return new QueueEntry(
                PlayerId.fromString(parts[0]),
                new QueueId(parts[1]),
                Instant.ofEpochMilli(Long.parseLong(parts[2])),
                MatchmakingProfile.valueOf(parts[3]),
                QueueType.valueOf(parts[4]),
                PlayerType.valueOf(parts[5]),
                parts[6]
        );
    }

    private String serialize(QueueEntry entry) {
        return entry.playerId() + "|"
                + entry.queueId() + "|"
                + entry.joinedAt().toEpochMilli() + "|"
                + entry.matchmakingProfile().name() + "|"
                + entry.queueType().name() + "|"
                + entry.playerType().name() + "|"
                + entry.sourceServerId();
    }
}
