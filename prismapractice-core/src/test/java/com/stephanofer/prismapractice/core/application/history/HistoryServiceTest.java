package com.stephanofer.prismapractice.core.application.history;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.history.*;
import com.stephanofer.prismapractice.api.match.*;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.rating.RatingApplyResult;
import com.stephanofer.prismapractice.api.rating.RatingChange;
import com.stephanofer.prismapractice.api.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {

    private final MatchId matchId = MatchId.random();
    private final PlayerId winner = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final PlayerId loser = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private final MatchSession completedMatch = new MatchSession(
            matchId,
            Instant.parse("2026-04-30T00:00:00Z"),
            Instant.parse("2026-04-30T00:00:05Z"),
            Instant.parse("2026-04-30T00:05:05Z"),
            MatchStatus.COMPLETED,
            PlayerType.ONE_VS_ONE,
            List.of(
                    new MatchParticipant(winner, MatchSide.LEFT, com.stephanofer.prismapractice.api.state.PlayerStatus.IN_QUEUE, true),
                    new MatchParticipant(loser, MatchSide.RIGHT, com.stephanofer.prismapractice.api.state.PlayerStatus.IN_QUEUE, true)
            ),
            new ArenaId("duel-sa-01"),
            com.stephanofer.prismapractice.api.arena.ArenaType.DUEL,
            new RegionId("sa"),
            RuntimeType.MATCH,
            "match-sa-01",
            new EffectiveMatchConfig(new ModeId("sword"), new QueueId("ranked-sword-1v1"), QueueType.RANKED, MatchmakingProfile.QUALITY_FIRST, 2, 5, 900, "sword-kit", List.of(), true, SeriesFormat.BEST_OF_THREE),
            new MatchScore(2, 1, 4),
            winner,
            loser,
            MatchResultType.NORMAL_WIN,
            "",
            "",
            false
    );
    private InMemoryMatchHistoryRepository repository;
    private HistoryService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMatchHistoryRepository();
        service = new HistoryService(repository);
    }

    @Test
    void shouldRecordCompletedMatch() {
        MatchHistoryRecordRequest request = new MatchHistoryRecordRequest(
                completedMatch,
                RatingApplyResult.success(
                        new RatingChange(matchId, winner, new ModeId("sword"), 1000, 1018, 18, "gold", "gold", 1000, 1018, Instant.parse("2026-04-30T00:05:06Z")),
                        new RatingChange(matchId, loser, new ModeId("sword"), 1000, 982, -18, "gold", "iron", 1000, 982, Instant.parse("2026-04-30T00:05:06Z"))
                ),
                List.of(
                        new MatchHistoryPlayerSnapshot(winner, "Winner", MatchSide.LEFT, true, null, null, 23, 7.5, InventorySnapshot.empty(), Map.of("gapple", 1), List.of("speed")),
                        new MatchHistoryPlayerSnapshot(loser, "Loser", MatchSide.RIGHT, false, null, null, 27, 0.0, InventorySnapshot.empty(), Map.of(), List.of())
                ),
                List.of(new MatchHistoryStatEntry("MATCH", "duration_seconds", "300")),
                List.of(new MatchHistoryEvent(0, MatchHistoryEventType.ROUND_START, null, null, "{}"))
        );

        HistoryRecordResult result = service.recordCompletedMatch(request);

        assertTrue(result.success());
        assertEquals(winner, result.summary().winnerPlayerId());
        assertEquals(1018, result.summary().players().get(0).srAfter());
    }

    @Test
    void shouldRejectDuplicateHistory() {
        MatchHistorySummary summary = new MatchHistorySummary(matchId, new ModeId("sword"), QueueType.RANKED, SeriesFormat.BEST_OF_THREE, new ArenaId("duel-sa-01"), new RegionId("sa"), "match-sa-01", Instant.parse("2026-04-30T00:00:00Z"), Instant.parse("2026-04-30T00:00:05Z"), Instant.parse("2026-04-30T00:05:05Z"), 300, winner, List.of(), List.of(), List.of());
        repository.save(summary);

        HistoryRecordResult result = service.recordCompletedMatch(new MatchHistoryRecordRequest(completedMatch, null, List.of(
                new MatchHistoryPlayerSnapshot(winner, "Winner", MatchSide.LEFT, true, null, null, 23, 7.5, InventorySnapshot.empty(), Map.of(), List.of()),
                new MatchHistoryPlayerSnapshot(loser, "Loser", MatchSide.RIGHT, false, null, null, 27, 0.0, InventorySnapshot.empty(), Map.of(), List.of())
        ), List.of(), List.of()));

        assertEquals(HistoryRecordFailureReason.MATCH_ALREADY_RECORDED, result.failureReason());
    }

    @Test
    void shouldRejectIncompleteSnapshots() {
        HistoryRecordResult result = service.recordCompletedMatch(new MatchHistoryRecordRequest(completedMatch, null, List.of(
                new MatchHistoryPlayerSnapshot(winner, "Winner", MatchSide.LEFT, true, null, null, 23, 7.5, InventorySnapshot.empty(), Map.of(), List.of())
        ), List.of(), List.of()));

        assertEquals(HistoryRecordFailureReason.PLAYER_SNAPSHOTS_INCOMPLETE, result.failureReason());
    }

    private static final class InMemoryMatchHistoryRepository implements MatchHistoryRepository {
        private final Map<String, MatchHistorySummary> values = new ConcurrentHashMap<>();
        @Override public boolean exists(MatchId matchId) { return values.containsKey(matchId.toString()); }
        @Override public MatchHistorySummary save(MatchHistorySummary summary) { values.put(summary.matchId().toString(), summary); return summary; }
        @Override public Optional<MatchHistorySummary> findByMatchId(MatchId matchId) { return Optional.ofNullable(values.get(matchId.toString())); }
        @Override public List<MatchHistorySummary> findRecentByPlayerId(PlayerId playerId, int limit, int offset) { return values.values().stream().filter(v -> v.players().stream().anyMatch(p -> p.playerId().equals(playerId))).toList(); }
    }
}
