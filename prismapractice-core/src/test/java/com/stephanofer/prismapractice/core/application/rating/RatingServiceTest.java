package com.stephanofer.prismapractice.core.application.rating;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.*;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.rating.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class RatingServiceTest {

    private final MatchId matchId = MatchId.random();
    private final ModeId modeId = new ModeId("sword");
    private final PlayerId winner = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final PlayerId loser = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    private InMemoryMatchRepository matchRepository;
    private InMemoryModeRatingRepository modeRatingRepository;
    private InMemoryGlobalRatingRepository globalRatingRepository;
    private InMemoryRatingChangeRepository ratingChangeRepository;
    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        matchRepository = new InMemoryMatchRepository();
        modeRatingRepository = new InMemoryModeRatingRepository();
        globalRatingRepository = new InMemoryGlobalRatingRepository();
        ratingChangeRepository = new InMemoryRatingChangeRepository();
        InMemoryRankTierRepository rankTierRepository = new InMemoryRankTierRepository();
        InMemorySeasonContextRepository seasonContextRepository = new InMemorySeasonContextRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneOffset.UTC);
        ratingService = new RatingService(matchRepository, modeRatingRepository, globalRatingRepository, rankTierRepository, seasonContextRepository, ratingChangeRepository, clock);

        matchRepository.save(new MatchSession(
                matchId,
                Instant.parse("2026-04-29T23:50:00Z"),
                Instant.parse("2026-04-29T23:50:10Z"),
                Instant.parse("2026-04-29T23:55:00Z"),
                MatchStatus.COMPLETED,
                com.stephanofer.prismapractice.api.common.PlayerType.ONE_VS_ONE,
                List.of(
                        new MatchParticipant(winner, MatchSide.LEFT, com.stephanofer.prismapractice.api.state.PlayerStatus.IN_QUEUE, true),
                        new MatchParticipant(loser, MatchSide.RIGHT, com.stephanofer.prismapractice.api.state.PlayerStatus.IN_QUEUE, true)
                ),
                new com.stephanofer.prismapractice.api.arena.ArenaId("duel-sa-01"),
                com.stephanofer.prismapractice.api.arena.ArenaType.DUEL,
                new com.stephanofer.prismapractice.api.matchmaking.RegionId("sa"),
                com.stephanofer.prismapractice.api.common.RuntimeType.MATCH,
                "match-sa-01",
                new EffectiveMatchConfig(modeId, new com.stephanofer.prismapractice.api.common.QueueId("ranked-sword-1v1"), QueueType.RANKED, MatchmakingProfile.QUALITY_FIRST, 2, 5, 900, "sword-kit", List.of(), true, SeriesFormat.BEST_OF_THREE),
                MatchScore.initial(),
                winner,
                loser,
                MatchResultType.NORMAL_WIN,
                "",
                "",
                false
        ));
    }

    @Test
    void shouldApplyRatingAndUpdateGlobal() {
        RatingApplyResult result = ratingService.applyPostMatchRating(new RatingApplyRequest(matchId, modeId, QueueType.RANKED, winner, loser, true, true, true, ""));

        assertTrue(result.success());
        assertTrue(result.winnerChange().afterSr() > result.winnerChange().beforeSr());
        assertTrue(result.loserChange().afterSr() < result.loserChange().beforeSr());
        assertTrue(globalRatingRepository.find(winner, "season-1").isPresent());
    }

    @Test
    void shouldBeIdempotentPerMatchPlayer() {
        ratingService.applyPostMatchRating(new RatingApplyRequest(matchId, modeId, QueueType.RANKED, winner, loser, true, true, true, ""));

        RatingApplyResult second = ratingService.applyPostMatchRating(new RatingApplyRequest(matchId, modeId, QueueType.RANKED, winner, loser, true, true, true, ""));

        assertEquals(RatingApplyFailureReason.ALREADY_APPLIED, second.failureReason());
    }

    @Test
    void shouldCompletePlacementsAfterTenGames() {
        ModeRating existing = new ModeRating(winner, modeId, 1200, "gold", 1200, "gold", false, 9, "season-1", Instant.parse("2026-04-29T23:00:00Z"), 4, 5);
        modeRatingRepository.save(existing);

        ratingService.applyPostMatchRating(new RatingApplyRequest(matchId, modeId, QueueType.RANKED, winner, loser, true, true, true, ""));

        ModeRating updated = modeRatingRepository.find(winner, modeId, "season-1").orElseThrow();
        assertTrue(updated.placementsCompleted());
        assertEquals(10, updated.placementsPlayed());
    }

    private static final class InMemoryMatchRepository implements MatchRepository {
        private final Map<String, MatchSession> values = new ConcurrentHashMap<>();
        @Override public Optional<MatchSession> findById(MatchId matchId) { return Optional.ofNullable(values.get(matchId.toString())); }
        @Override public MatchSession save(MatchSession matchSession) { values.put(matchSession.matchId().toString(), matchSession); return matchSession; }
    }

    private static final class InMemoryModeRatingRepository implements ModeRatingRepository {
        private final Map<String, ModeRating> values = new ConcurrentHashMap<>();
        @Override public Optional<ModeRating> find(PlayerId playerId, ModeId modeId, String seasonId) { return Optional.ofNullable(values.get(playerId + ":" + modeId + ":" + seasonId)); }
        @Override public List<ModeRating> findByPlayerId(PlayerId playerId, String seasonId) { return values.values().stream().filter(v -> v.playerId().equals(playerId) && v.seasonId().equals(seasonId)).toList(); }
        @Override public List<ModeRating> findByModeId(ModeId modeId, String seasonId) { return values.values().stream().filter(v -> v.modeId().equals(modeId) && v.seasonId().equals(seasonId)).toList(); }
        @Override public ModeRating save(ModeRating modeRating) { values.put(modeRating.playerId() + ":" + modeRating.modeId() + ":" + modeRating.seasonId(), modeRating); return modeRating; }
    }

    private static final class InMemoryGlobalRatingRepository implements GlobalRatingRepository {
        private final Map<String, GlobalRatingSnapshot> values = new ConcurrentHashMap<>();
        @Override public Optional<GlobalRatingSnapshot> find(PlayerId playerId, String seasonId) { return Optional.ofNullable(values.get(playerId + ":" + seasonId)); }
        @Override public List<GlobalRatingSnapshot> findBySeasonId(String seasonId) { return values.values().stream().filter(v -> v.seasonId().equals(seasonId)).toList(); }
        @Override public GlobalRatingSnapshot save(GlobalRatingSnapshot snapshot) { values.put(snapshot.playerId() + ":" + snapshot.seasonId(), snapshot); return snapshot; }
    }

    private static final class InMemoryRankTierRepository implements RankTierRepository {
        @Override public List<RankTier> findEnabled() {
            return List.of(
                    new RankTier("iron", "Iron", 0, 999, 1, true),
                    new RankTier("gold", "Gold", 1000, 1299, 2, true),
                    new RankTier("emerald", "Emerald", 1300, 1599, 3, true),
                    new RankTier("diamond", "Diamond", 1600, 1899, 4, true),
                    new RankTier("netherite", "Netherite", 1900, 2199, 5, true),
                    new RankTier("master", "Master", 2200, 2399, 6, true),
                    new RankTier("grandmaster", "Grandmaster", 2400, null, 7, true)
            );
        }
    }

    private static final class InMemorySeasonContextRepository implements SeasonContextRepository {
        @Override public Optional<SeasonContext> findActive() { return Optional.of(new SeasonContext("season-1", "Season 1", "ACTIVE", Instant.parse("2026-04-01T00:00:00Z"), null)); }
    }

    private static final class InMemoryRatingChangeRepository implements RatingChangeRepository {
        private final List<RatingChange> values = new ArrayList<>();
        @Override public boolean exists(MatchId matchId, PlayerId playerId) { return values.stream().anyMatch(v -> v.matchId().equals(matchId) && v.playerId().equals(playerId)); }
        @Override public RatingChange save(RatingChange ratingChange) { values.add(ratingChange); return ratingChange; }
    }
}
