package com.stephanofer.prismapractice.core.application.match;

import com.stephanofer.prismapractice.api.arena.ArenaReservation;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.*;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.arena.ArenaAllocationService;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class MatchService {

    private final MatchRepository matchRepository;
    private final ActiveMatchRepository activeMatchRepository;
    private final ArenaAllocationService arenaAllocationService;
    private final MatchmakingService matchmakingService;
    private final PlayerStateService playerStateService;
    private final Clock clock;

    public MatchService(
            MatchRepository matchRepository,
            ActiveMatchRepository activeMatchRepository,
            ArenaAllocationService arenaAllocationService,
            MatchmakingService matchmakingService,
            PlayerStateService playerStateService,
            Clock clock
    ) {
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository");
        this.activeMatchRepository = Objects.requireNonNull(activeMatchRepository, "activeMatchRepository");
        this.arenaAllocationService = Objects.requireNonNull(arenaAllocationService, "arenaAllocationService");
        this.matchmakingService = Objects.requireNonNull(matchmakingService, "matchmakingService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public MatchResult createMatch(MatchCreationRequest request) {
        Objects.requireNonNull(request, "request");
        if (isProposalStale(request)) {
            arenaAllocationService.releaseReservation(request.reservation().arenaId(), request.reservation().reservationId());
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.failure(MatchCreateFailureReason.PROPOSAL_STALE);
        }
        if (!isReservationValid(request.reservation(), request)) {
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.failure(MatchCreateFailureReason.RESERVATION_INVALID);
        }
        if (activeMatchRepository.findByPlayerId(request.proposal().leftPlayerId()).isPresent()
                || activeMatchRepository.findByPlayerId(request.proposal().rightPlayerId()).isPresent()) {
            arenaAllocationService.releaseReservation(request.reservation().arenaId(), request.reservation().reservationId());
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.failure(MatchCreateFailureReason.PLAYER_ALREADY_IN_ACTIVE_MATCH);
        }
        if (!arenaAllocationService.markReservationInUse(request.reservation().arenaId(), request.reservation().reservationId())) {
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.failure(MatchCreateFailureReason.ARENA_CONFIRMATION_FAILED);
        }

        try {
            Instant now = Instant.now(clock);
            MatchSession session = new MatchSession(
                    MatchId.random(),
                    now,
                    null,
                    null,
                    MatchStatus.CREATED,
                    request.playerType(),
                    List.of(
                            new MatchParticipant(request.proposal().leftPlayerId(), MatchSide.LEFT, PlayerStatus.IN_QUEUE, true),
                            new MatchParticipant(request.proposal().rightPlayerId(), MatchSide.RIGHT, PlayerStatus.IN_QUEUE, true)
                    ),
                    request.reservation().arenaId(),
                    request.arenaType(),
                    request.reservation().regionId(),
                    request.runtimeType(),
                    "",
                    request.effectiveConfig(),
                    MatchScore.initial(),
                    null,
                    null,
                    null,
                    "",
                    "",
                    true
            );
            MatchSession persisted = matchRepository.save(session);
            activeMatchRepository.save(new ActiveMatch(
                    persisted.matchId(),
                    persisted.arenaId(),
                    request.proposal().leftPlayerId(),
                    request.proposal().rightPlayerId(),
                    MatchStatus.CREATED,
                    "",
                    now
            ));
            playerStateService.transition(request.proposal().leftPlayerId(), PlayerStatus.TRANSFERRING, PlayerSubStatus.NONE);
            playerStateService.transition(request.proposal().rightPlayerId(), PlayerStatus.TRANSFERRING, PlayerSubStatus.NONE);
            MatchSession transferring = updateStatus(persisted, MatchStatus.TRANSFERRING, null, null, "", "", true, "");
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.success(transferring);
        } catch (RuntimeException exception) {
            arenaAllocationService.releaseReservation(request.reservation().arenaId(), request.reservation().reservationId());
            matchmakingService.releaseProposalLocks(request.proposal());
            return MatchResult.failure(MatchCreateFailureReason.PERSISTENCE_FAILURE);
        }
    }

    public MatchSession markWaitingPlayers(MatchId matchId, String runtimeServerId) {
        MatchSession session = requireMatch(matchId);
        return saveWithActive(updateStatus(session, MatchStatus.WAITING_PLAYERS, null, null, session.cancelReason(), session.failureReason(), session.recoverable(), runtimeServerId));
    }

    public MatchSession startPreFight(MatchId matchId) {
        return saveWithActive(updateStatus(requireMatch(matchId), MatchStatus.PRE_FIGHT, null, null, "", "", true, requireMatch(matchId).runtimeServerId()));
    }

    public MatchSession startMatch(MatchId matchId) {
        MatchSession session = requireMatch(matchId);
        MatchSession updated = new MatchSession(
                session.matchId(), session.createdAt(), Instant.now(clock), session.endedAt(), MatchStatus.IN_PROGRESS,
                session.playerType(), session.participants(), session.arenaId(), session.arenaType(), session.regionId(),
                session.runtimeType(), session.runtimeServerId(), session.effectiveConfig(), session.score(), session.winnerPlayerId(),
                session.loserPlayerId(), session.resultType(), session.cancelReason(), session.failureReason(), session.recoverable()
        );
        PlayerId left = session.participants().get(0).playerId();
        PlayerId right = session.participants().get(1).playerId();
        playerStateService.transition(left, PlayerStatus.IN_MATCH, PlayerSubStatus.IN_ROUND);
        playerStateService.transition(right, PlayerStatus.IN_MATCH, PlayerSubStatus.IN_ROUND);
        return saveWithActive(updated);
    }

    public MatchSession finishRound(MatchId matchId, MatchSide winnerSide) {
        MatchSession session = requireMatch(matchId);
        MatchScore newScore = session.score().withRoundWinner(winnerSide);
        MatchSession updated = new MatchSession(
                session.matchId(), session.createdAt(), session.startedAt(), session.endedAt(), MatchStatus.ROUND_ENDING,
                session.playerType(), session.participants(), session.arenaId(), session.arenaType(), session.regionId(),
                session.runtimeType(), session.runtimeServerId(), session.effectiveConfig(), newScore, session.winnerPlayerId(),
                session.loserPlayerId(), session.resultType(), session.cancelReason(), session.failureReason(), session.recoverable()
        );
        return saveWithActive(updated);
    }

    public MatchSession completeMatch(MatchId matchId, MatchCompletionRequest request) {
        Objects.requireNonNull(request, "request");
        MatchSession session = requireMatch(matchId);
        MatchSession updated = new MatchSession(
                session.matchId(), session.createdAt(), session.startedAt(), Instant.now(clock), MatchStatus.COMPLETED,
                session.playerType(), session.participants(), session.arenaId(), session.arenaType(), session.regionId(),
                session.runtimeType(), session.runtimeServerId(), session.effectiveConfig(), session.score(), request.winnerPlayerId(),
                request.loserPlayerId(), request.resultType(), "", "", false
        );
        MatchSession saved = matchRepository.save(updated);
        cleanupAfterTerminal(saved, true);
        return saved;
    }

    public MatchSession cancelMatch(MatchId matchId, String reason) {
        MatchSession session = requireMatch(matchId);
        MatchSession updated = updateStatus(session, MatchStatus.CANCELLED, MatchResultType.CANCELLED, null, reason == null ? "cancelled" : reason, "", true, session.runtimeServerId());
        MatchSession saved = matchRepository.save(updated);
        cleanupAfterTerminal(saved, true);
        return saved;
    }

    public MatchSession failMatch(MatchId matchId, String reason) {
        MatchSession session = requireMatch(matchId);
        MatchSession updated = updateStatus(session, MatchStatus.FAILED, MatchResultType.FAILED, null, session.cancelReason(), reason == null ? "failed" : reason, true, session.runtimeServerId());
        MatchSession saved = matchRepository.save(updated);
        arenaAllocationService.markArenaBroken(saved.arenaId(), "", reason);
        cleanupAfterTerminal(saved, false);
        return saved;
    }

    private MatchSession saveWithActive(MatchSession session) {
        MatchSession saved = matchRepository.save(session);
        activeMatchRepository.save(new ActiveMatch(saved.matchId(), saved.arenaId(), saved.participants().get(0).playerId(), saved.participants().get(1).playerId(), saved.status(), saved.runtimeServerId(), Instant.now(clock)));
        return saved;
    }

    private void cleanupAfterTerminal(MatchSession session, boolean releaseArena) {
        PlayerId left = session.participants().get(0).playerId();
        PlayerId right = session.participants().get(1).playerId();
        activeMatchRepository.remove(session.matchId(), left, right);
        playerStateService.transition(left, PlayerStatus.HUB, PlayerSubStatus.NONE);
        playerStateService.transition(right, PlayerStatus.HUB, PlayerSubStatus.NONE);
        if (releaseArena) {
            arenaAllocationService.releaseReservation(session.arenaId(), "");
        }
    }

    private MatchSession requireMatch(MatchId matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new IllegalStateException("Match not found: " + matchId));
    }

    private boolean isProposalStale(MatchCreationRequest request) {
        return stateNotQueue(request.proposal().leftPlayerId()) || stateNotQueue(request.proposal().rightPlayerId());
    }

    private boolean stateNotQueue(PlayerId playerId) {
        return playerStateService.findCurrentState(playerId).map(state -> state.status() != PlayerStatus.IN_QUEUE).orElse(true);
    }

    private boolean isReservationValid(ArenaReservation reservation, MatchCreationRequest request) {
        return reservation.leftPlayerId().equals(request.proposal().leftPlayerId())
                && reservation.rightPlayerId().equals(request.proposal().rightPlayerId())
                && reservation.queueId().equals(request.proposal().queueId())
                && reservation.expiresAt().isAfter(Instant.now(clock));
    }

    private MatchSession updateStatus(MatchSession session, MatchStatus status, MatchResultType resultType, MatchScore score, String cancelReason, String failureReason, boolean recoverable, String runtimeServerId) {
        return new MatchSession(
                session.matchId(), session.createdAt(), session.startedAt(), status == MatchStatus.COMPLETED || status == MatchStatus.CANCELLED || status == MatchStatus.FAILED ? Instant.now(clock) : session.endedAt(),
                status, session.playerType(), session.participants(), session.arenaId(), session.arenaType(), session.regionId(), session.runtimeType(),
                runtimeServerId, session.effectiveConfig(), score == null ? session.score() : score, session.winnerPlayerId(), session.loserPlayerId(),
                resultType == null ? session.resultType() : resultType, cancelReason, failureReason, recoverable
        );
    }
}
