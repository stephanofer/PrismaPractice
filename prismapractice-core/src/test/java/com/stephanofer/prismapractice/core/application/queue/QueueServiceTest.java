package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.matchmaking.PingRangePreference;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.PlayerPartyIndexRepository;
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
import com.stephanofer.prismapractice.api.state.PracticePresence;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueServiceTest {

    private final PlayerId playerId = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
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

    private InMemoryPlayerStateRepository stateRepository;
    private InMemoryPresenceRepository presenceRepository;
    private InMemoryLockRepository lockRepository;
    private InMemoryQueueEntryRepository queueEntryRepository;
    private InMemoryMatchmakingSnapshotRepository snapshotRepository;
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        stateRepository = new InMemoryPlayerStateRepository();
        presenceRepository = new InMemoryPresenceRepository();
        lockRepository = new InMemoryLockRepository();
        queueEntryRepository = new InMemoryQueueEntryRepository();
        snapshotRepository = new InMemoryMatchmakingSnapshotRepository();
        QueueRepository queueRepository = new InMemoryQueueRepository(queueDefinition);
        ProfileRepository profileRepository = new InMemoryProfileRepository();
        PlayerPartyIndexRepository partyIndexRepository = player -> false;
        Clock clock = Clock.fixed(Instant.parse("2026-04-29T18:00:00Z"), ZoneOffset.UTC);
        PlayerStateService playerStateService = new PlayerStateService(stateRepository, presenceRepository, lockRepository, clock);
        queueService = new QueueService(queueRepository, queueEntryRepository, snapshotRepository, profileRepository, partyIndexRepository, playerStateService, lockRepository, clock);

        stateRepository.save(PlayerState.hub(playerId, Instant.now(clock)));
        presenceRepository.save(PracticePresence.online(playerId, RuntimeType.HUB, "hub-1", Instant.now(clock)));
    }

    @Test
    void shouldJoinQueueFromHub() {
        QueueJoinResult result = queueService.joinQueue(playerId, queueId, "hub-1");

        assertTrue(result.success());
        assertNotNull(result.entry());
        assertEquals(PlayerStatus.IN_QUEUE, result.state().status());
        assertEquals(PlayerSubStatus.WAITING_MATCH, result.state().subStatus());
        assertTrue(snapshotRepository.findByPlayerId(playerId).isPresent());
    }

    @Test
    void shouldRejectJoinWhileAlreadyQueued() {
        queueEntryRepository.save(new QueueEntry(
                playerId,
                queueId,
                Instant.parse("2026-04-29T18:00:00Z"),
                MatchmakingProfile.QUALITY_FIRST,
                QueueType.RANKED,
                PlayerType.ONE_VS_ONE,
                "hub-1"
        ));
        stateRepository.save(new PlayerState(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.parse("2026-04-29T18:00:00Z")));

        QueueJoinResult result = queueService.joinQueue(playerId, queueId, "hub-1");

        assertEquals(QueueJoinFailureReason.PLAYER_ALREADY_IN_QUEUE, result.failureReason());
    }

    @Test
    void shouldRepairStaleQueueEntryBeforeJoining() {
        queueEntryRepository.save(new QueueEntry(
                playerId,
                queueId,
                Instant.parse("2026-04-29T18:00:00Z"),
                MatchmakingProfile.QUALITY_FIRST,
                QueueType.RANKED,
                PlayerType.ONE_VS_ONE,
                "hub-1"
        ));
        stateRepository.save(PlayerState.hub(playerId, Instant.parse("2026-04-29T18:00:00Z")));

        QueueJoinResult result = queueService.joinQueue(playerId, queueId, "hub-1");

        assertTrue(result.success());
        assertEquals(PlayerStatus.IN_QUEUE, result.state().status());
    }

    @Test
    void shouldRepairStaleStateOnLeave() {
        stateRepository.save(new PlayerState(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.parse("2026-04-29T18:00:00Z")));

        QueueLeaveResult result = queueService.leaveQueue(playerId);

        assertEquals(QueueLeaveFailureReason.NOT_IN_QUEUE, result.failureReason());
        assertTrue(result.repairedState());
        assertEquals(PlayerStatus.HUB, result.state().status());
    }

    private static final class InMemoryProfileRepository implements ProfileRepository {
        @Override
        public Optional<PracticeProfile> findProfile(PlayerId playerId) {
            return Optional.of(PracticeProfile.bootstrap(playerId, "Stephanofer", Instant.parse("2026-04-29T18:00:00Z")));
        }

        @Override
        public PracticeProfile saveProfile(PracticeProfile profile) {
            return profile;
        }

        @Override
        public Optional<PracticeSettings> findSettings(PlayerId playerId) {
            return Optional.of(new PracticeSettings(playerId, true, true, false, true, true, true, true, true, PingRangePreference.WITHIN_100));
        }

        @Override
        public PracticeSettings saveSettings(PracticeSettings settings) {
            return settings;
        }
    }

    private static final class InMemoryQueueRepository implements QueueRepository {
        private final QueueDefinition queueDefinition;

        private InMemoryQueueRepository(QueueDefinition queueDefinition) {
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
        private final Map<String, QueueEntry> byPlayer = new ConcurrentHashMap<>();

        @Override
        public Optional<QueueEntry> findByPlayerId(PlayerId playerId) {
            return Optional.ofNullable(byPlayer.get(playerId.toString()));
        }

        @Override
        public java.util.List<QueueEntry> findByQueueId(QueueId queueId) {
            return byPlayer.values().stream().filter(entry -> entry.queueId().equals(queueId)).toList();
        }

        @Override
        public QueueEntry save(QueueEntry entry) {
            byPlayer.put(entry.playerId().toString(), entry);
            return entry;
        }

        @Override
        public boolean removeByPlayerId(PlayerId playerId) {
            return byPlayer.remove(playerId.toString()) != null;
        }

        @Override
        public boolean remove(QueueId queueId, PlayerId playerId) {
            QueueEntry entry = byPlayer.get(playerId.toString());
            if (entry == null || !entry.queueId().equals(queueId)) {
                return false;
            }
            byPlayer.remove(playerId.toString());
            return true;
        }
    }

    private static final class InMemoryPlayerStateRepository implements PlayerStateRepository {
        private final Map<String, PlayerState> states = new ConcurrentHashMap<>();

        @Override
        public Optional<PlayerState> find(PlayerId playerId) {
            return Optional.ofNullable(states.get(playerId.toString()));
        }

        @Override
        public PlayerState save(PlayerState state) {
            states.put(state.playerId().toString(), state);
            return state;
        }

        @Override
        public void delete(PlayerId playerId) {
            states.remove(playerId.toString());
        }
    }

    private static final class InMemoryMatchmakingSnapshotRepository implements MatchmakingSnapshotRepository {
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
            MatchmakingSnapshot snapshot = values.get(playerId.toString());
            if (snapshot == null || !snapshot.queueId().equals(queueId)) {
                return false;
            }
            values.remove(playerId.toString());
            return true;
        }
    }

    private static final class InMemoryPresenceRepository implements PlayerPresenceRepository {
        private final Map<String, PracticePresence> values = new ConcurrentHashMap<>();

        @Override
        public Optional<PracticePresence> find(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId.toString()));
        }

        @Override
        public PracticePresence save(PracticePresence presence) {
            values.put(presence.playerId().toString(), presence);
            return presence;
        }

        @Override
        public void delete(PlayerId playerId) {
            values.remove(playerId.toString());
        }
    }

    private static final class InMemoryLockRepository implements PlayerOperationLockRepository {
        private final Map<String, String> locks = new ConcurrentHashMap<>();

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
            return acquire("matchmaking:" + queueId + ':' + playerId);
        }

        @Override
        public boolean releaseMatchmakingLock(QueueId queueId, PlayerId playerId, String token) {
            return release("matchmaking:" + queueId + ':' + playerId, token);
        }

        @Override
        public Optional<String> acquireArenaLock(ArenaId arenaId) {
            return acquire("arena:" + arenaId);
        }

        @Override
        public boolean releaseArenaLock(ArenaId arenaId, String token) {
            return release("arena:" + arenaId, token);
        }

        private Optional<String> acquire(String key) {
            String token = UUID.randomUUID().toString();
            return locks.putIfAbsent(key, token) == null ? Optional.of(token) : Optional.empty();
        }

        private boolean release(String key, String token) {
            return locks.remove(key, token);
        }
    }
}
