package com.stephanofer.prismapractice.core.application.history;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.history.MatchHistoryPlayerSnapshot;
import com.stephanofer.prismapractice.api.history.MatchHistoryRecordRequest;
import com.stephanofer.prismapractice.api.history.MatchHistoryRepository;
import com.stephanofer.prismapractice.api.history.MatchHistorySummary;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.match.MatchSession;
import com.stephanofer.prismapractice.api.match.MatchStatus;
import com.stephanofer.prismapractice.api.rating.RatingApplyResult;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class HistoryService {

    private final MatchHistoryRepository matchHistoryRepository;

    public HistoryService(MatchHistoryRepository matchHistoryRepository) {
        this.matchHistoryRepository = Objects.requireNonNull(matchHistoryRepository, "matchHistoryRepository");
    }

    public HistoryRecordResult recordCompletedMatch(MatchHistoryRecordRequest request) {
        Objects.requireNonNull(request, "request");
        MatchSession match = request.matchSession();
        if (match.status() != MatchStatus.COMPLETED || match.endedAt() == null) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.MATCH_NOT_COMPLETED);
        }
        if (matchHistoryRepository.exists(match.matchId())) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.MATCH_ALREADY_RECORDED);
        }
        if (match.startedAt() == null || match.endedAt().isBefore(match.startedAt())) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.INVALID_MATCH_TIMESTAMPS);
        }
        if (request.players().size() != match.participants().size()) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.PLAYER_SNAPSHOTS_INCOMPLETE);
        }
        Set<PlayerId> participantIds = match.participants().stream().map(p -> p.playerId()).collect(Collectors.toSet());
        Set<PlayerId> snapshotIds = request.players().stream().map(MatchHistoryPlayerSnapshot::playerId).collect(Collectors.toSet());
        if (!participantIds.equals(snapshotIds)) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.PLAYER_SNAPSHOTS_INCONSISTENT);
        }
        if (match.winnerPlayerId() == null || !participantIds.contains(match.winnerPlayerId())) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.INVALID_WINNER);
        }

        List<MatchHistoryPlayerSnapshot> normalizedPlayers = normalizePlayers(request.players(), request.ratingApplyResult(), match.winnerPlayerId());
        long durationSeconds = Duration.between(match.startedAt(), match.endedAt()).toSeconds();
        MatchHistorySummary summary = new MatchHistorySummary(
                match.matchId(),
                match.effectiveConfig().modeId(),
                match.effectiveConfig().queueType(),
                match.effectiveConfig().seriesFormat(),
                match.arenaId(),
                match.regionId(),
                match.runtimeServerId().isBlank() ? "unknown" : match.runtimeServerId(),
                match.createdAt(),
                match.startedAt(),
                match.endedAt(),
                durationSeconds,
                match.winnerPlayerId(),
                normalizedPlayers,
                request.stats(),
                request.events()
        );
        try {
            return HistoryRecordResult.success(matchHistoryRepository.save(summary));
        } catch (RuntimeException exception) {
            return HistoryRecordResult.failure(HistoryRecordFailureReason.PERSISTENCE_FAILURE);
        }
    }

    public Optional<MatchHistorySummary> getMatchHistory(MatchId matchId) {
        Objects.requireNonNull(matchId, "matchId");
        return matchHistoryRepository.findByMatchId(matchId);
    }

    public List<MatchHistorySummary> getRecentHistory(PlayerId playerId, int limit, int offset) {
        Objects.requireNonNull(playerId, "playerId");
        if (limit <= 0 || limit > 100 || offset < 0) {
            throw new IllegalArgumentException("Invalid recent history query window");
        }
        return matchHistoryRepository.findRecentByPlayerId(playerId, limit, offset);
    }

    private List<MatchHistoryPlayerSnapshot> normalizePlayers(List<MatchHistoryPlayerSnapshot> players, RatingApplyResult ratingApplyResult, PlayerId winnerPlayerId) {
        return players.stream().map(snapshot -> {
            Integer srBefore = snapshot.srBefore();
            Integer srAfter = snapshot.srAfter();
            if (ratingApplyResult != null && ratingApplyResult.success()) {
                if (ratingApplyResult.winnerChange() != null && ratingApplyResult.winnerChange().playerId().equals(snapshot.playerId())) {
                    srBefore = ratingApplyResult.winnerChange().beforeSr();
                    srAfter = ratingApplyResult.winnerChange().afterSr();
                } else if (ratingApplyResult.loserChange() != null && ratingApplyResult.loserChange().playerId().equals(snapshot.playerId())) {
                    srBefore = ratingApplyResult.loserChange().beforeSr();
                    srAfter = ratingApplyResult.loserChange().afterSr();
                }
            }
            return new MatchHistoryPlayerSnapshot(
                    snapshot.playerId(),
                    snapshot.playerName(),
                    snapshot.side(),
                    snapshot.playerId().equals(winnerPlayerId),
                    srBefore,
                    srAfter,
                    snapshot.pingFinal(),
                    snapshot.finalHealth(),
                    snapshot.inventorySnapshot(),
                    snapshot.remainingConsumables(),
                    snapshot.activeEffects()
            );
        }).toList();
    }
}
