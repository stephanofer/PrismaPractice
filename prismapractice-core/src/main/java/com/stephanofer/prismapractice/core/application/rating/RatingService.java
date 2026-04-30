package com.stephanofer.prismapractice.core.application.rating;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchRepository;
import com.stephanofer.prismapractice.api.match.MatchResultType;
import com.stephanofer.prismapractice.api.match.MatchStatus;
import com.stephanofer.prismapractice.api.rating.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class RatingService {

    private static final int DEFAULT_SR = 1000;
    private static final int PLACEMENTS_REQUIRED = 10;

    private final MatchRepository matchRepository;
    private final ModeRatingRepository modeRatingRepository;
    private final GlobalRatingRepository globalRatingRepository;
    private final RankTierRepository rankTierRepository;
    private final SeasonContextRepository seasonContextRepository;
    private final RatingChangeRepository ratingChangeRepository;
    private final RatingCalculator ratingCalculator;
    private final GlobalRatingCalculator globalRatingCalculator;
    private final Clock clock;

    public RatingService(
            MatchRepository matchRepository,
            ModeRatingRepository modeRatingRepository,
            GlobalRatingRepository globalRatingRepository,
            RankTierRepository rankTierRepository,
            SeasonContextRepository seasonContextRepository,
            RatingChangeRepository ratingChangeRepository,
            Clock clock
    ) {
        this(matchRepository, modeRatingRepository, globalRatingRepository, rankTierRepository, seasonContextRepository, ratingChangeRepository,
                new RatingCalculator(), new GlobalRatingCalculator(), clock);
    }

    public RatingService(
            MatchRepository matchRepository,
            ModeRatingRepository modeRatingRepository,
            GlobalRatingRepository globalRatingRepository,
            RankTierRepository rankTierRepository,
            SeasonContextRepository seasonContextRepository,
            RatingChangeRepository ratingChangeRepository,
            RatingCalculator ratingCalculator,
            GlobalRatingCalculator globalRatingCalculator,
            Clock clock
    ) {
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository");
        this.modeRatingRepository = Objects.requireNonNull(modeRatingRepository, "modeRatingRepository");
        this.globalRatingRepository = Objects.requireNonNull(globalRatingRepository, "globalRatingRepository");
        this.rankTierRepository = Objects.requireNonNull(rankTierRepository, "rankTierRepository");
        this.seasonContextRepository = Objects.requireNonNull(seasonContextRepository, "seasonContextRepository");
        this.ratingChangeRepository = Objects.requireNonNull(ratingChangeRepository, "ratingChangeRepository");
        this.ratingCalculator = Objects.requireNonNull(ratingCalculator, "ratingCalculator");
        this.globalRatingCalculator = Objects.requireNonNull(globalRatingCalculator, "globalRatingCalculator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RatingApplyResult applyPostMatchRating(RatingApplyRequest request) {
        Objects.requireNonNull(request, "request");
        if (!request.affectsSr()) {
            return RatingApplyResult.failure(RatingApplyFailureReason.MATCH_NOT_ELIGIBLE);
        }
        if (request.winnerPlayerId().equals(request.loserPlayerId())) {
            return RatingApplyResult.failure(RatingApplyFailureReason.INVALID_WINNER_LOSER);
        }
        if (ratingChangeRepository.exists(request.matchId(), request.winnerPlayerId())
                || ratingChangeRepository.exists(request.matchId(), request.loserPlayerId())) {
            return RatingApplyResult.failure(RatingApplyFailureReason.ALREADY_APPLIED);
        }
        var match = matchRepository.findById(request.matchId()).orElse(null);
        if (match == null || match.status() != MatchStatus.COMPLETED || match.resultType() == null || match.resultType() == MatchResultType.CANCELLED || match.resultType() == MatchResultType.FAILED) {
            return RatingApplyResult.failure(RatingApplyFailureReason.MATCH_NOT_ELIGIBLE);
        }
        List<RankTier> tiers = rankTierRepository.findEnabled().stream().sorted(Comparator.comparingInt(RankTier::sortOrder)).toList();
        if (tiers.isEmpty()) {
            return RatingApplyResult.failure(RatingApplyFailureReason.RANK_TIERS_MISSING);
        }
        String seasonId = resolveSeasonId(request);
        if (seasonId == null) {
            return RatingApplyResult.failure(RatingApplyFailureReason.SEASON_CONTEXT_MISSING);
        }

        ModeRating winner = modeRatingRepository.find(request.winnerPlayerId(), request.modeId(), seasonId).orElseGet(() -> bootstrapModeRating(request.winnerPlayerId(), request.modeId(), seasonId, tiers));
        ModeRating loser = modeRatingRepository.find(request.loserPlayerId(), request.modeId(), seasonId).orElseGet(() -> bootstrapModeRating(request.loserPlayerId(), request.modeId(), seasonId, tiers));
        GlobalRatingSnapshot winnerGlobalBefore = globalRatingRepository.find(updatedPlayerId(winner), seasonId)
                .orElse(new GlobalRatingSnapshot(updatedPlayerId(winner), DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), seasonId, Instant.now(clock)));
        GlobalRatingSnapshot loserGlobalBefore = globalRatingRepository.find(updatedPlayerId(loser), seasonId)
                .orElse(new GlobalRatingSnapshot(updatedPlayerId(loser), DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), seasonId, Instant.now(clock)));

        var winnerDelta = ratingCalculator.calculateWinnerDelta(winner, loser);
        var loserDelta = ratingCalculator.calculateLoserDelta(loser, winner);
        ModeRating updatedWinner = applyModeRatingUpdate(winner, winnerDelta.delta(), true, tiers);
        ModeRating updatedLoser = applyModeRatingUpdate(loser, loserDelta.delta(), false, tiers);
        modeRatingRepository.save(updatedWinner);
        modeRatingRepository.save(updatedLoser);

        GlobalRatingSnapshot winnerGlobal = recalcGlobal(updatedWinner.playerId(), seasonId, tiers);
        GlobalRatingSnapshot loserGlobal = recalcGlobal(updatedLoser.playerId(), seasonId, tiers);

        Instant now = Instant.now(clock);
        RatingChange winnerChange = ratingChangeRepository.save(new RatingChange(
                request.matchId(), updatedWinner.playerId(), updatedWinner.modeId(), winner.currentSr(), updatedWinner.currentSr(),
                updatedWinner.currentSr() - winner.currentSr(), winner.currentRankKey(), updatedWinner.currentRankKey(),
                winnerGlobalBefore.currentGlobalRating(), winnerGlobal.currentGlobalRating(), now
        ));
        RatingChange loserChange = ratingChangeRepository.save(new RatingChange(
                request.matchId(), updatedLoser.playerId(), updatedLoser.modeId(), loser.currentSr(), updatedLoser.currentSr(),
                updatedLoser.currentSr() - loser.currentSr(), loser.currentRankKey(), updatedLoser.currentRankKey(),
                loserGlobalBefore.currentGlobalRating(), loserGlobal.currentGlobalRating(), now
        ));
        return RatingApplyResult.success(winnerChange, loserChange);
    }

    private PlayerId updatedPlayerId(ModeRating rating) {
        return rating.playerId();
    }

    private String resolveSeasonId(RatingApplyRequest request) {
        if (!request.seasonId().isBlank()) {
            return request.seasonId();
        }
        return seasonContextRepository.findActive().map(SeasonContext::seasonId).orElse(null);
    }

    private ModeRating bootstrapModeRating(PlayerId playerId, com.stephanofer.prismapractice.api.common.ModeId modeId, String seasonId, List<RankTier> tiers) {
        String rankKey = resolveRank(DEFAULT_SR, tiers).rankKey();
        return new ModeRating(playerId, modeId, DEFAULT_SR, rankKey, DEFAULT_SR, rankKey, false, 0, seasonId, Instant.now(clock), 0, 0);
    }

    private ModeRating applyModeRatingUpdate(ModeRating current, int delta, boolean win, List<RankTier> tiers) {
        int newSr = Math.max(0, current.currentSr() + delta);
        RankTier newTier = resolveRank(newSr, tiers);
        int peakSr = Math.max(current.peakSr(), newSr);
        RankTier peakTier = resolveRank(peakSr, tiers);
        int placementsPlayed = current.placementsPlayed() + 1;
        boolean placementsCompleted = placementsPlayed >= PLACEMENTS_REQUIRED;
        return new ModeRating(
                current.playerId(),
                current.modeId(),
                newSr,
                newTier.rankKey(),
                peakSr,
                peakTier.rankKey(),
                placementsCompleted,
                placementsPlayed,
                current.seasonId(),
                Instant.now(clock),
                current.wins() + (win ? 1 : 0),
                current.losses() + (win ? 0 : 1)
        );
    }

    private GlobalRatingSnapshot recalcGlobal(PlayerId playerId, String seasonId, List<RankTier> tiers) {
        List<ModeRating> ratings = modeRatingRepository.findByPlayerId(playerId, seasonId);
        int global = globalRatingCalculator.calculate(ratings);
        RankTier tier = resolveRank(global, tiers);
        GlobalRatingSnapshot current = globalRatingRepository.find(playerId, seasonId)
                .orElse(new GlobalRatingSnapshot(playerId, DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), DEFAULT_SR, resolveRank(DEFAULT_SR, tiers).rankKey(), seasonId, Instant.now(clock)));
        int peak = Math.max(current.peakGlobalRating(), global);
        RankTier peakTier = resolveRank(peak, tiers);
        GlobalRatingSnapshot updated = new GlobalRatingSnapshot(playerId, global, tier.rankKey(), peak, peakTier.rankKey(), seasonId, Instant.now(clock));
        return globalRatingRepository.save(updated);
    }

    private RankTier resolveRank(int sr, List<RankTier> tiers) {
        return tiers.stream().filter(tier -> tier.matches(sr)).findFirst().orElseGet(() -> tiers.getLast());
    }
}
