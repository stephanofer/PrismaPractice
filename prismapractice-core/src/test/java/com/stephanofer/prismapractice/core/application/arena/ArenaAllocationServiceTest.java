package com.stephanofer.prismapractice.core.application.arena;

import com.stephanofer.prismapractice.api.arena.*;
import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ArenaAllocationServiceTest {

    private final PlayerId left = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final PlayerId right = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private final QueueId queueId = new QueueId("ranked-sword-1v1");
    private final ArenaId arenaId = new ArenaId("duel-sa-01");
    private InMemoryArenaRepository arenaRepository;
    private InMemoryArenaOperationalStateRepository operationalStateRepository;
    private InMemoryArenaReservationRepository reservationRepository;
    private InMemoryPlayerStateRepository playerStateRepository;
    private InMemoryLockRepository lockRepository;
    private ArenaAllocationService service;
    private MatchmakingProposal proposal;

    @BeforeEach
    void setUp() {
        arenaRepository = new InMemoryArenaRepository();
        operationalStateRepository = new InMemoryArenaOperationalStateRepository();
        reservationRepository = new InMemoryArenaReservationRepository();
        playerStateRepository = new InMemoryPlayerStateRepository();
        lockRepository = new InMemoryLockRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-04-29T19:00:00Z"), ZoneOffset.UTC);
        PlayerStateService playerStateService = new PlayerStateService(playerStateRepository, new NoopPresenceRepository(), lockRepository, clock);
        MatchmakingService matchmakingService = org.mockito.Mockito.mock(MatchmakingService.class);
        service = new ArenaAllocationService(arenaRepository, operationalStateRepository, reservationRepository, matchmakingService, playerStateService, lockRepository, clock, Duration.ofSeconds(30));

        arenaRepository.save(new ArenaDefinition(arenaId, "Duel SA 01", ArenaType.DUEL, new RegionId("sa"), RuntimeType.MATCH,
                "match-sa", Set.of(new ModeId("sword")), Set.of(), Set.of(PlayerType.ONE_VS_ONE), true, true, 100, true));
        arenaRepository.save(new ArenaDefinition(new ArenaId("duel-sa-02"), "Duel SA 02", ArenaType.DUEL, new RegionId("sa"), RuntimeType.MATCH,
                "match-sa", Set.of(new ModeId("sword")), Set.of(), Set.of(PlayerType.ONE_VS_ONE), true, true, 90, false));

        playerStateRepository.save(new PlayerState(left, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.now(clock)));
        playerStateRepository.save(new PlayerState(right, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.now(clock)));
        proposal = new MatchmakingProposal(queueId, left, right, new RegionId("sa"), 80,
                new MatchmakingSearchWindow("ranked-0-15", Duration.ZERO, 75, 72), "test", "left-lock", "right-lock");
    }

    @Test
    void shouldAllocateBestArenaAndReserveIt() {
        ArenaAllocationResult result = service.allocate(new ArenaAllocationRequest(proposal, ArenaType.DUEL, new ModeId("sword"), PlayerType.ONE_VS_ONE, RuntimeType.MATCH, new RegionId("sa")));

        assertTrue(result.success());
        assertEquals(arenaId, result.arena().arenaId());
        assertNotNull(result.reservation());
        assertEquals(ArenaOperationalStatus.RESERVED, operationalStateRepository.find(arenaId).orElseThrow().status());
    }

    @Test
    void shouldSkipBusyArenaAndUseNextAvailable() {
        reservationRepository.save(new ArenaReservation("existing", arenaId, queueId, left, right, new RegionId("sa"), RuntimeType.MATCH, Instant.parse("2026-04-29T18:59:50Z"), Instant.parse("2026-04-29T19:00:25Z")));
        operationalStateRepository.save(new ArenaOperationalState(arenaId, ArenaOperationalStatus.RESERVED, "existing", Instant.parse("2026-04-29T18:59:50Z"), ""));

        ArenaAllocationResult result = service.allocate(new ArenaAllocationRequest(proposal, ArenaType.DUEL, new ModeId("sword"), PlayerType.ONE_VS_ONE, RuntimeType.MATCH, new RegionId("sa")));

        assertTrue(result.success());
        assertEquals(new ArenaId("duel-sa-02"), result.arena().arenaId());
    }

    @Test
    void shouldRejectStaleProposal() {
        playerStateRepository.save(new PlayerState(left, PlayerStatus.HUB, PlayerSubStatus.NONE, Instant.parse("2026-04-29T19:00:00Z")));

        ArenaAllocationResult result = service.allocate(new ArenaAllocationRequest(proposal, ArenaType.DUEL, new ModeId("sword"), PlayerType.ONE_VS_ONE, RuntimeType.MATCH, new RegionId("sa")));

        assertEquals(ArenaAllocationFailureReason.PROPOSAL_STALE, result.failureReason());
    }

    private static final class InMemoryArenaRepository implements ArenaRepository {
        private final Map<String, ArenaDefinition> values = new ConcurrentHashMap<>();

        @Override
        public Optional<ArenaDefinition> findById(ArenaId arenaId) { return Optional.ofNullable(values.get(arenaId.toString())); }

        @Override
        public List<ArenaDefinition> findCompatible(ArenaType arenaType, ModeId modeId, PlayerType playerType, RuntimeType runtimeType, RegionId regionId) {
            return values.values().stream().filter(arena -> arena.arenaType() == arenaType && arena.runtimeType() == runtimeType && arena.regionId().equals(regionId)).toList();
        }

        @Override
        public ArenaDefinition save(ArenaDefinition arenaDefinition) { values.put(arenaDefinition.arenaId().toString(), arenaDefinition); return arenaDefinition; }
    }

    private static final class InMemoryArenaOperationalStateRepository implements ArenaOperationalStateRepository {
        private final Map<String, ArenaOperationalState> values = new ConcurrentHashMap<>();
        @Override public Optional<ArenaOperationalState> find(ArenaId arenaId) { return Optional.ofNullable(values.get(arenaId.toString())); }
        @Override public ArenaOperationalState save(ArenaOperationalState state) { values.put(state.arenaId().toString(), state); return state; }
    }

    private static final class InMemoryArenaReservationRepository implements ArenaReservationRepository {
        private final Map<String, ArenaReservation> values = new ConcurrentHashMap<>();
        @Override public Optional<ArenaReservation> findByArenaId(ArenaId arenaId) { return Optional.ofNullable(values.get(arenaId.toString())); }
        @Override public ArenaReservation save(ArenaReservation reservation) { values.put(reservation.arenaId().toString(), reservation); return reservation; }
        @Override public boolean remove(ArenaId arenaId, String reservationId) {
            ArenaReservation current = values.get(arenaId.toString());
            if (current == null || (!reservationId.isBlank() && !current.reservationId().equals(reservationId))) return false;
            values.remove(arenaId.toString());
            return true;
        }
    }

    private static final class InMemoryPlayerStateRepository implements PlayerStateRepository {
        private final Map<String, PlayerState> values = new ConcurrentHashMap<>();
        @Override public Optional<PlayerState> find(PlayerId playerId) { return Optional.ofNullable(values.get(playerId.toString())); }
        @Override public PlayerState save(PlayerState state) { values.put(state.playerId().toString(), state); return state; }
        @Override public void delete(PlayerId playerId) { values.remove(playerId.toString()); }
    }

    private static final class InMemoryLockRepository implements PlayerOperationLockRepository {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        @Override public Optional<String> acquireTransitionLock(PlayerId playerId) { return acquire("t:" + playerId); }
        @Override public boolean releaseTransitionLock(PlayerId playerId, String token) { return release("t:" + playerId, token); }
        @Override public Optional<String> acquireMatchmakingLock(QueueId queueId, PlayerId playerId) { return acquire("m:" + queueId + ':' + playerId); }
        @Override public boolean releaseMatchmakingLock(QueueId queueId, PlayerId playerId, String token) { return release("m:" + queueId + ':' + playerId, token); }
        @Override public Optional<String> acquireArenaLock(ArenaId arenaId) { return acquire("a:" + arenaId); }
        @Override public boolean releaseArenaLock(ArenaId arenaId, String token) { return release("a:" + arenaId, token); }
        private Optional<String> acquire(String key) { String token = UUID.randomUUID().toString(); return values.putIfAbsent(key, token) == null ? Optional.of(token) : Optional.empty(); }
        private boolean release(String key, String token) { return values.remove(key, token); }
    }

    private static final class NoopPresenceRepository implements PlayerPresenceRepository {
        @Override public Optional<com.stephanofer.prismapractice.api.state.PracticePresence> find(PlayerId playerId) { return Optional.empty(); }
        @Override public com.stephanofer.prismapractice.api.state.PracticePresence save(com.stephanofer.prismapractice.api.state.PracticePresence presence) { return presence; }
        @Override public void delete(PlayerId playerId) { }
    }
}
