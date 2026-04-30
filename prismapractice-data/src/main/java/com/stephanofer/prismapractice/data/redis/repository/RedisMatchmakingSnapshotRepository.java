package com.stephanofer.prismapractice.data.redis.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.matchmaking.PingRangePreference;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.matchmaking.RegionPing;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.data.redis.RedisStorage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RedisMatchmakingSnapshotRepository implements MatchmakingSnapshotRepository {

    private final RedisStorage redisStorage;

    public RedisMatchmakingSnapshotRepository(RedisStorage redisStorage) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage");
    }

    @Override
    public Optional<MatchmakingSnapshot> findByPlayerId(PlayerId playerId) {
        String queueId = redisStorage.syncCommands().get(redisStorage.keyspace().playerQueue(playerId.toString()));
        if (queueId == null || queueId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(read(new QueueId(queueId), playerId));
    }

    @Override
    public List<MatchmakingSnapshot> findByQueueId(QueueId queueId) {
        List<String> playerIds = redisStorage.syncCommands().zrange(redisStorage.keyspace().queueEntries(queueId.toString()), 0, -1);
        List<MatchmakingSnapshot> snapshots = new ArrayList<>();
        for (String rawPlayerId : playerIds) {
            MatchmakingSnapshot snapshot = read(queueId, PlayerId.fromString(rawPlayerId));
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return List.copyOf(snapshots);
    }

    @Override
    public MatchmakingSnapshot save(MatchmakingSnapshot snapshot) {
        redisStorage.syncCommands().set(
                redisStorage.keyspace().matchmakingSnapshot(snapshot.queueId().toString(), snapshot.playerId().toString()),
                serialize(snapshot)
        );
        return snapshot;
    }

    @Override
    public boolean removeByPlayerId(PlayerId playerId) {
        Optional<MatchmakingSnapshot> existing = findByPlayerId(playerId);
        return existing.isPresent() && remove(existing.get().queueId(), playerId);
    }

    @Override
    public boolean remove(QueueId queueId, PlayerId playerId) {
        Long removed = redisStorage.syncCommands().del(redisStorage.keyspace().matchmakingSnapshot(queueId.toString(), playerId.toString()));
        return removed != null && removed > 0;
    }

    private MatchmakingSnapshot read(QueueId queueId, PlayerId playerId) {
        String raw = redisStorage.syncCommands().get(redisStorage.keyspace().matchmakingSnapshot(queueId.toString(), playerId.toString()));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 9) {
            remove(queueId, playerId);
            return null;
        }
        return new MatchmakingSnapshot(
                PlayerId.fromString(parts[0]),
                new QueueId(parts[1]),
                Instant.ofEpochMilli(Long.parseLong(parts[2])),
                PlayerStatus.valueOf(parts[3]),
                Integer.parseInt(parts[4]),
                PingRangePreference.valueOf(parts[5]),
                parseRegionPings(parts[6]),
                parts[7],
                Instant.ofEpochMilli(Long.parseLong(parts[8]))
        );
    }

    private String serialize(MatchmakingSnapshot snapshot) {
        return snapshot.playerId() + "|"
                + snapshot.queueId() + "|"
                + snapshot.joinedAt().toEpochMilli() + "|"
                + snapshot.stateAtCapture().name() + "|"
                + snapshot.skillValue() + "|"
                + snapshot.pingRangePreference().name() + "|"
                + formatRegionPings(snapshot.regionPings()) + "|"
                + snapshot.sourceServerId() + "|"
                + snapshot.capturedAt().toEpochMilli();
    }

    private String formatRegionPings(List<RegionPing> regionPings) {
        return regionPings.stream()
                .map(regionPing -> regionPing.regionId().value() + ':' + regionPing.pingMillis())
                .reduce((left, right) -> left + ',' + right)
                .orElse("");
    }

    private List<RegionPing> parseRegionPings(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        List<RegionPing> values = new ArrayList<>();
        for (String regionPart : raw.split(",")) {
            String[] parts = regionPart.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            values.add(new RegionPing(new RegionId(parts[0]), Integer.parseInt(parts[1])));
        }
        return List.copyOf(values);
    }
}
