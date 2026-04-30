package com.stephanofer.prismapractice.core.application.match;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaReservation;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.match.*;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.arena.ArenaAllocationService;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private final PlayerId left = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final PlayerId right = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private final MatchmakingProposal proposal = new MatchmakingProposal(
            new com.stephanofer.prismapractice.api.common.QueueId("ranked-sword-1v1"), left, right, new RegionId("sa"), 88,
            new MatchmakingSearchWindow("ranked-0-15", Duration.ZERO, 75, 72), "test", "l", "r"
    );
    private final ArenaReservation reservation = new ArenaReservation(
            "res-1", new ArenaId("duel-sa-01"), proposal.queueId(), left, right, new RegionId("sa"), RuntimeType.MATCH,
            Instant.parse("2026-04-29T22:00:00Z"), Instant.parse("2026-04-29T22:00:30Z")
    );

    private InMemoryMatchRepository matchRepository;
    private InMemoryActiveMatchRepository activeMatchRepository;
    private InMemoryPlayerStateRepository stateRepository;
    private MatchService service;
    private ArenaAllocationService arenaAllocationService;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchRepository = new InMemoryMatchRepository();
        activeMatchRepository = new InMemoryActiveMatchRepository();
        stateRepository = new InMemoryPlayerStateRepository();
        InMemoryLockRepository lockRepository = new InMemoryLockRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-04-29T22:00:05Z"), ZoneOffset.UTC);
        PlayerStateService playerStateService = new PlayerStateService(stateRepository, new NoopPresenceRepository(), lockRepository, clock);
        arenaAllocationService = Mockito.mock(ArenaAllocationService.class);
        matchmakingService = Mockito.mock(MatchmakingService.class);
        service = new MatchService(matchRepository, activeMatchRepository, arenaAllocationService, matchmakingService, playerStateService, clock);

        stateRepository.save(new PlayerState(left, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.now(clock)));
        stateRepository.save(new PlayerState(right, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.now(clock)));
        when(arenaAllocationService.markReservationInUse(any(), anyString())).thenReturn(true);
        when(arenaAllocationService.releaseReservation(any(), anyString())).thenReturn(true);
    }

    @Test
    void shouldCreateAndStartMatchLifecycle() {
        MatchCreationRequest request = new MatchCreationRequest(proposal, reservation, com.stephanofer.prismapractice.api.arena.ArenaType.DUEL,
                com.stephanofer.prismapractice.api.common.PlayerType.ONE_VS_ONE, RuntimeType.MATCH,
                new EffectiveMatchConfig(new com.stephanofer.prismapractice.api.common.ModeId("sword"), proposal.queueId(), QueueType.RANKED, MatchmakingProfile.QUALITY_FIRST,
                        2, 5, 900, "sword-kit", List.of("NO_FALL_DAMAGE"), true, SeriesFormat.BEST_OF_THREE));

        MatchResult created = service.createMatch(request);
        assertTrue(created.success());
        assertEquals(MatchStatus.TRANSFERRING, created.session().status());
        assertTrue(activeMatchRepository.findByMatchId(created.session().matchId()).isPresent());

        MatchSession waiting = service.markWaitingPlayers(created.session().matchId(), "match-sa-01");
        assertEquals(MatchStatus.WAITING_PLAYERS, waiting.status());

        MatchSession preFight = service.startPreFight(created.session().matchId());
        assertEquals(MatchStatus.PRE_FIGHT, preFight.status());

        MatchSession started = service.startMatch(created.session().matchId());
        assertEquals(MatchStatus.IN_PROGRESS, started.status());
        assertNotNull(started.startedAt());
    }

    @Test
    void shouldCompleteMatchAndCleanup() {
        MatchCreationRequest request = new MatchCreationRequest(proposal, reservation, com.stephanofer.prismapractice.api.arena.ArenaType.DUEL,
                com.stephanofer.prismapractice.api.common.PlayerType.ONE_VS_ONE, RuntimeType.MATCH,
                new EffectiveMatchConfig(new com.stephanofer.prismapractice.api.common.ModeId("sword"), proposal.queueId(), QueueType.RANKED, MatchmakingProfile.QUALITY_FIRST,
                        1, 3, 600, "sword-kit", List.of(), true, SeriesFormat.BEST_OF_ONE));
        MatchSession created = service.createMatch(request).session();
        service.markWaitingPlayers(created.matchId(), "match-sa-01");
        service.startPreFight(created.matchId());
        service.startMatch(created.matchId());

        MatchSession completed = service.completeMatch(created.matchId(), new MatchCompletionRequest(left, right, MatchResultType.NORMAL_WIN));

        assertEquals(MatchStatus.COMPLETED, completed.status());
        assertEquals(left, completed.winnerPlayerId());
        assertTrue(activeMatchRepository.findByMatchId(created.matchId()).isEmpty());
    }

    @Test
    void shouldRejectStaleProposalAtCreate() {
        stateRepository.save(new PlayerState(left, PlayerStatus.HUB, PlayerSubStatus.NONE, Instant.parse("2026-04-29T22:00:05Z")));
        MatchCreationRequest request = new MatchCreationRequest(proposal, reservation, com.stephanofer.prismapractice.api.arena.ArenaType.DUEL,
                com.stephanofer.prismapractice.api.common.PlayerType.ONE_VS_ONE, RuntimeType.MATCH,
                new EffectiveMatchConfig(new com.stephanofer.prismapractice.api.common.ModeId("sword"), proposal.queueId(), QueueType.RANKED, MatchmakingProfile.QUALITY_FIRST,
                        1, 3, 600, "sword-kit", List.of(), true, SeriesFormat.BEST_OF_ONE));

        MatchResult result = service.createMatch(request);
        assertFalse(result.success());
        assertEquals(MatchCreateFailureReason.PROPOSAL_STALE, result.createFailureReason());
    }

    private static final class InMemoryMatchRepository implements MatchRepository {
        private final Map<String, MatchSession> values = new ConcurrentHashMap<>();
        @Override public Optional<MatchSession> findById(MatchId matchId) { return Optional.ofNullable(values.get(matchId.toString())); }
        @Override public MatchSession save(MatchSession matchSession) { values.put(matchSession.matchId().toString(), matchSession); return matchSession; }
    }

    private static final class InMemoryActiveMatchRepository implements ActiveMatchRepository {
        private final Map<String, ActiveMatch> byMatch = new ConcurrentHashMap<>();
        private final Map<String, ActiveMatch> byPlayer = new ConcurrentHashMap<>();
        @Override public Optional<ActiveMatch> findByMatchId(MatchId matchId) { return Optional.ofNullable(byMatch.get(matchId.toString())); }
        @Override public Optional<ActiveMatch> findByPlayerId(PlayerId playerId) { return Optional.ofNullable(byPlayer.get(playerId.toString())); }
        @Override public ActiveMatch save(ActiveMatch activeMatch) { byMatch.put(activeMatch.matchId().toString(), activeMatch); byPlayer.put(activeMatch.leftPlayerId().toString(), activeMatch); byPlayer.put(activeMatch.rightPlayerId().toString(), activeMatch); return activeMatch; }
        @Override public void remove(MatchId matchId, PlayerId leftPlayerId, PlayerId rightPlayerId) { byMatch.remove(matchId.toString()); byPlayer.remove(leftPlayerId.toString()); byPlayer.remove(rightPlayerId.toString()); }
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
        @Override public Optional<String> acquireMatchmakingLock(com.stephanofer.prismapractice.api.common.QueueId queueId, PlayerId playerId) { return acquire("m:" + queueId + ':' + playerId); }
        @Override public boolean releaseMatchmakingLock(com.stephanofer.prismapractice.api.common.QueueId queueId, PlayerId playerId, String token) { return release("m:" + queueId + ':' + playerId, token); }
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
