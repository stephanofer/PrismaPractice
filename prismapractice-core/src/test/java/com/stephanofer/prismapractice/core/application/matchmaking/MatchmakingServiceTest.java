package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.matchmaking.PingRangePreference;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.matchmaking.RegionPing;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.queue.SearchExpansionStrategy;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchmakingServiceTest {

    private final QueueId queueId = new QueueId("ranked-sword-1v1");
    private final QueueDefinition queueDefinition = new QueueDefinition(
            queueId,
            new ModeId("sword"),
            "Ranked Sword 1v1",
            QueueType.RANKED,
            PlayerType.ONE_VS_ONE,
            MatchmakingProfile.QUALITY_FIRST,
            true,
            true,
            true,
            true,
            true,
            true,
            SearchExpansionStrategy.RANKED_STANDARD,
            true,
            Set.of(PlayerStatus.HUB),
            Set.of(PlayerStatus.IN_MATCH, PlayerStatus.IN_QUEUE, PlayerStatus.TRANSFERRING),
            true,
            true,
            true,
            true,
            RuntimeType.MATCH
    );

    private InMemoryQueueEntryRepository queueEntryRepository;
    private InMemorySnapshotRepository snapshotRepository;
    private InMemoryPlayerStateRepository stateRepository;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        queueEntryRepository = new InMemoryQueueEntryRepository();
        snapshotRepository = new InMemorySnapshotRepository();
        stateRepository = new InMemoryPlayerStateRepository();
        InMemoryLockRepository lockRepository = new InMemoryLockRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-04-29T18:00:40Z"), ZoneOffset.UTC);
        PlayerStateService playerStateService = new PlayerStateService(stateRepository, new NoopPresenceRepository(), lockRepository, clock);
        matchmakingService = new MatchmakingService(new SingleQueueRepository(queueDefinition), queueEntryRepository, snapshotRepository, playerStateService, lockRepository, clock);
    }

    @Test
    void shouldPickBestValidProposal() {
        PlayerId first = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        PlayerId second = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        PlayerId third = new PlayerId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        enqueue(first, 1000, Instant.parse("2026-04-29T18:00:00Z"), PingRangePreference.WITHIN_100, 42, 39);
        enqueue(second, 1005, Instant.parse("2026-04-29T18:00:05Z"), PingRangePreference.WITHIN_100, 41, 40);
        enqueue(third, 1450, Instant.parse("2026-04-29T18:00:10Z"), PingRangePreference.WITHIN_100, 120, 118);

        Optional<MatchmakingProposal> proposal = matchmakingService.findBestProposal(queueId);

        assertTrue(proposal.isPresent());
        assertEquals(first, proposal.get().leftPlayerId());
        assertEquals(second, proposal.get().rightPlayerId());
        assertEquals("na", proposal.get().selectedRegion().value());
    }

    @Test
    void shouldRejectPairWhenPingRangeIsIncompatible() {
        PlayerId first = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        PlayerId second = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        enqueue(first, 1000, Instant.parse("2026-04-29T18:00:00Z"), PingRangePreference.WITHIN_25, 20, 20);
        enqueue(second, 1005, Instant.parse("2026-04-29T18:00:03Z"), PingRangePreference.WITHIN_25, 90, 90);

        Optional<MatchmakingProposal> proposal = matchmakingService.findBestProposal(queueId);

        assertTrue(proposal.isEmpty());
    }

    private void enqueue(PlayerId playerId, int skill, Instant joinedAt, PingRangePreference pingRangePreference, int saPing, int naPing) {
        queueEntryRepository.save(new QueueEntry(playerId, queueId, joinedAt, MatchmakingProfile.QUALITY_FIRST, QueueType.RANKED, PlayerType.ONE_VS_ONE, "hub-1"));
        snapshotRepository.save(new MatchmakingSnapshot(
                playerId,
                queueId,
                joinedAt,
                PlayerStatus.IN_QUEUE,
                skill,
                pingRangePreference,
                List.of(new RegionPing(new RegionId("sa"), saPing), new RegionPing(new RegionId("na"), naPing)),
                "hub-1",
                joinedAt
        ));
        stateRepository.save(new PlayerState(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, joinedAt));
    }

    private static final class SingleQueueRepository implements QueueRepository {
        private final QueueDefinition queueDefinition;

        private SingleQueueRepository(QueueDefinition queueDefinition) {
            this.queueDefinition = queueDefinition;
        }

        @Override
        public Optional<QueueDefinition> findById(QueueId queueId) {
            return this.queueDefinition.queueId().equals(queueId) ? Optional.of(queueDefinition) : Optional.empty();
        }

        @Override
        public QueueDefinition save(QueueDefinition queueDefinition) {
            return queueDefinition;
        }
    }

    private static final class InMemoryQueueEntryRepository implements QueueEntryRepository {
        private final Map<String, QueueEntry> values = new ConcurrentHashMap<>();

        @Override
        public Optional<QueueEntry> findByPlayerId(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId.toString()));
        }

        @Override
        public List<QueueEntry> findByQueueId(QueueId queueId) {
            return values.values().stream().filter(entry -> entry.queueId().equals(queueId)).toList();
        }

        @Override
        public QueueEntry save(QueueEntry entry) {
            values.put(entry.playerId().toString(), entry);
            return entry;
        }

        @Override
        public boolean removeByPlayerId(PlayerId playerId) {
            return values.remove(playerId.toString()) != null;
        }

        @Override
        public boolean remove(QueueId queueId, PlayerId playerId) {
            QueueEntry current = values.get(playerId.toString());
            if (current == null || !current.queueId().equals(queueId)) {
                return false;
            }
            values.remove(playerId.toString());
            return true;
        }
    }

    private static final class InMemorySnapshotRepository implements MatchmakingSnapshotRepository {
        private final Map<String, MatchmakingSnapshot> values = new ConcurrentHashMap<>();

        @Override
        public Optional<MatchmakingSnapshot> findByPlayerId(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId.toString()));
        }

        @Override
        public List<MatchmakingSnapshot> findByQueueId(QueueId queueId) {
            return values.values().stream().filter(snapshot -> snapshot.queueId().equals(queueId)).toList();
        }

        @Override
        public MatchmakingSnapshot save(MatchmakingSnapshot snapshot) {
            values.put(snapshot.playerId().toString(), snapshot);
            return snapshot;
        }

        @Override
        public boolean removeByPlayerId(PlayerId playerId) {
            return values.remove(playerId.toString()) != null;
        }

        @Override
        public boolean remove(QueueId queueId, PlayerId playerId) {
            MatchmakingSnapshot current = values.get(playerId.toString());
            if (current == null || !current.queueId().equals(queueId)) {
                return false;
            }
            values.remove(playerId.toString());
            return true;
        }
    }

    private static final class InMemoryPlayerStateRepository implements PlayerStateRepository {
        private final Map<String, PlayerState> values = new ConcurrentHashMap<>();

        @Override
        public Optional<PlayerState> find(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId.toString()));
        }

        @Override
        public PlayerState save(PlayerState state) {
            values.put(state.playerId().toString(), state);
            return state;
        }

        @Override
        public void delete(PlayerId playerId) {
            values.remove(playerId.toString());
        }
    }

    private static final class InMemoryLockRepository implements PlayerOperationLockRepository {
        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public Optional<String> acquireTransitionLock(PlayerId playerId) {
            return acquire("transition:" + playerId);
        }

        @Override
        public boolean releaseTransitionLock(PlayerId playerId, String token) {
            return release("transition:" + playerId, token);
        }

        @Override
        public Optional<String> acquireMatchmakingLock(QueueId queueId, PlayerId playerId) {
            return acquire("mm:" + queueId + ':' + playerId);
        }

        @Override
        public boolean releaseMatchmakingLock(QueueId queueId, PlayerId playerId, String token) {
            return release("mm:" + queueId + ':' + playerId, token);
        }

        @Override
        public Optional<String> acquireArenaLock(ArenaId arenaId) {
            return acquire("a:" + arenaId);
        }

        @Override
        public boolean releaseArenaLock(ArenaId arenaId, String token) {
            return release("a:" + arenaId, token);
        }

        private Optional<String> acquire(String key) {
            String token = UUID.randomUUID().toString();
            return values.putIfAbsent(key, token) == null ? Optional.of(token) : Optional.empty();
        }

        private boolean release(String key, String token) {
            return values.remove(key, token);
        }
    }

    private static final class NoopPresenceRepository implements PlayerPresenceRepository {
        @Override
        public Optional<com.stephanofer.prismapractice.api.state.PracticePresence> find(PlayerId playerId) {
            return Optional.empty();
        }

        @Override
        public com.stephanofer.prismapractice.api.state.PracticePresence save(com.stephanofer.prismapractice.api.state.PracticePresence presence) {
            return presence;
        }

        @Override
        public void delete(PlayerId playerId) {
        }
    }
}
