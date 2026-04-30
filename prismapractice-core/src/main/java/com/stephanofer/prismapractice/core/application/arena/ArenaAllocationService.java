package com.stephanofer.prismapractice.core.application.arena;

import com.stephanofer.prismapractice.api.arena.ArenaAllocationFailureReason;
import com.stephanofer.prismapractice.api.arena.ArenaAllocationRequest;
import com.stephanofer.prismapractice.api.arena.ArenaAllocationResult;
import com.stephanofer.prismapractice.api.arena.ArenaDefinition;
import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalState;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalStateRepository;
import com.stephanofer.prismapractice.api.arena.ArenaOperationalStatus;
import com.stephanofer.prismapractice.api.arena.ArenaRepository;
import com.stephanofer.prismapractice.api.arena.ArenaReservation;
import com.stephanofer.prismapractice.api.arena.ArenaReservationRepository;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArenaAllocationService {

    private final ArenaRepository arenaRepository;
    private final ArenaOperationalStateRepository operationalStateRepository;
    private final ArenaReservationRepository reservationRepository;
    private final MatchmakingService matchmakingService;
    private final PlayerStateService playerStateService;
    private final PlayerOperationLockRepository lockRepository;
    private final ArenaCompatibilityPolicy compatibilityPolicy;
    private final ArenaSelectionPolicy selectionPolicy;
    private final Clock clock;
    private final Duration reservationTtl;

    public ArenaAllocationService(
            ArenaRepository arenaRepository,
            ArenaOperationalStateRepository operationalStateRepository,
            ArenaReservationRepository reservationRepository,
            MatchmakingService matchmakingService,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            Clock clock,
            Duration reservationTtl
    ) {
        this(arenaRepository, operationalStateRepository, reservationRepository, matchmakingService, playerStateService,
                lockRepository, new ArenaCompatibilityPolicy(), new ArenaSelectionPolicy(), clock, reservationTtl);
    }

    public ArenaAllocationService(
            ArenaRepository arenaRepository,
            ArenaOperationalStateRepository operationalStateRepository,
            ArenaReservationRepository reservationRepository,
            MatchmakingService matchmakingService,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            ArenaCompatibilityPolicy compatibilityPolicy,
            ArenaSelectionPolicy selectionPolicy,
            Clock clock,
            Duration reservationTtl
    ) {
        this.arenaRepository = Objects.requireNonNull(arenaRepository, "arenaRepository");
        this.operationalStateRepository = Objects.requireNonNull(operationalStateRepository, "operationalStateRepository");
        this.reservationRepository = Objects.requireNonNull(reservationRepository, "reservationRepository");
        this.matchmakingService = Objects.requireNonNull(matchmakingService, "matchmakingService");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.lockRepository = Objects.requireNonNull(lockRepository, "lockRepository");
        this.compatibilityPolicy = Objects.requireNonNull(compatibilityPolicy, "compatibilityPolicy");
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.reservationTtl = Objects.requireNonNull(reservationTtl, "reservationTtl");
    }

    public ArenaAllocationResult allocate(ArenaAllocationRequest request) {
        Objects.requireNonNull(request, "request");
        if (isProposalStale(request)) {
            return ArenaAllocationResult.failure(ArenaAllocationFailureReason.PROPOSAL_STALE);
        }

        List<ArenaDefinition> compatible = arenaRepository.findCompatible(
                request.arenaType(), request.modeId(), request.playerType(), request.targetRuntime(), request.requiredRegion())
                .stream()
                .filter(arena -> compatibilityPolicy.isCompatible(request, arena))
                .toList();
        if (compatible.isEmpty()) {
            return ArenaAllocationResult.failure(ArenaAllocationFailureReason.NO_COMPATIBLE_ARENA);
        }

        boolean hadBusyCandidate = false;
        for (ArenaDefinition arena : selectionPolicy.prioritize(compatible)) {
            ArenaAllocationResult attempt = tryReserve(request, arena);
            if (attempt.success()) {
                return attempt;
            }
            if (attempt.failureReason() == ArenaAllocationFailureReason.ARENA_ALREADY_RESERVED
                    || attempt.failureReason() == ArenaAllocationFailureReason.RESERVATION_CONFLICT) {
                hadBusyCandidate = true;
            }
        }
        return ArenaAllocationResult.failure(hadBusyCandidate
                ? ArenaAllocationFailureReason.NO_AVAILABLE_ARENA
                : ArenaAllocationFailureReason.RESERVATION_CONFLICT);
    }

    public boolean releaseReservation(ArenaId arenaId, String reservationId) {
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(reservationId, "reservationId");
        Optional<String> lockToken = lockRepository.acquireArenaLock(arenaId);
        if (lockToken.isEmpty()) {
            return false;
        }
        try {
            boolean removed = reservationRepository.remove(arenaId, reservationId);
            if (removed) {
                operationalStateRepository.save(ArenaOperationalState.available(arenaId, Instant.now(clock)));
            }
            return removed;
        } finally {
            lockRepository.releaseArenaLock(arenaId, lockToken.get());
        }
    }

    public boolean markReservationInUse(ArenaId arenaId, String reservationId) {
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(reservationId, "reservationId");
        Optional<String> lockToken = lockRepository.acquireArenaLock(arenaId);
        if (lockToken.isEmpty()) {
            return false;
        }
        try {
            Optional<ArenaReservation> reservation = reservationRepository.findByArenaId(arenaId);
            if (reservation.isEmpty() || !reservation.get().reservationId().equals(reservationId)) {
                return false;
            }
            operationalStateRepository.save(new ArenaOperationalState(arenaId, ArenaOperationalStatus.IN_USE, reservationId, Instant.now(clock), ""));
            return true;
        } finally {
            lockRepository.releaseArenaLock(arenaId, lockToken.get());
        }
    }

    public void markArenaBroken(ArenaId arenaId, String reservationId, String reason) {
        Objects.requireNonNull(arenaId, "arenaId");
        Optional<String> lockToken = lockRepository.acquireArenaLock(arenaId);
        if (lockToken.isEmpty()) {
            return;
        }
        try {
            reservationRepository.remove(arenaId, reservationId == null ? "" : reservationId);
            operationalStateRepository.save(new ArenaOperationalState(arenaId, ArenaOperationalStatus.BROKEN, "", Instant.now(clock), reason == null ? "broken" : reason));
        } finally {
            lockRepository.releaseArenaLock(arenaId, lockToken.get());
        }
    }

    private ArenaAllocationResult tryReserve(ArenaAllocationRequest request, ArenaDefinition arena) {
        Optional<String> lockToken = lockRepository.acquireArenaLock(arena.arenaId());
        if (lockToken.isEmpty()) {
            return ArenaAllocationResult.failure(ArenaAllocationFailureReason.ARENA_ALREADY_RESERVED);
        }
        try {
            ArenaOperationalState currentState = operationalStateRepository.find(arena.arenaId())
                    .orElse(ArenaOperationalState.available(arena.arenaId(), Instant.now(clock)));
            if (currentState.status() == ArenaOperationalStatus.BROKEN) {
                return ArenaAllocationResult.failure(ArenaAllocationFailureReason.ARENA_BROKEN);
            }
            if (currentState.status() == ArenaOperationalStatus.DISABLED) {
                return ArenaAllocationResult.failure(ArenaAllocationFailureReason.ARENA_DISABLED);
            }
            if (currentState.status() != ArenaOperationalStatus.AVAILABLE) {
                sanitizeOrRejectBusyArena(arena.arenaId(), currentState);
                return ArenaAllocationResult.failure(ArenaAllocationFailureReason.ARENA_ALREADY_RESERVED);
            }
            if (reservationRepository.findByArenaId(arena.arenaId()).isPresent()) {
                operationalStateRepository.save(new ArenaOperationalState(arena.arenaId(), ArenaOperationalStatus.RESERVED,
                        reservationRepository.findByArenaId(arena.arenaId()).get().reservationId(), Instant.now(clock), ""));
                return ArenaAllocationResult.failure(ArenaAllocationFailureReason.RESERVATION_CONFLICT);
            }

            Instant now = Instant.now(clock);
            String reservationId = UUID.randomUUID().toString();
            ArenaReservation reservation = reservationRepository.save(new ArenaReservation(
                    reservationId,
                    arena.arenaId(),
                    request.proposal().queueId(),
                    request.proposal().leftPlayerId(),
                    request.proposal().rightPlayerId(),
                    request.requiredRegion(),
                    request.targetRuntime(),
                    now,
                    now.plus(reservationTtl)
            ));
            operationalStateRepository.save(new ArenaOperationalState(arena.arenaId(), ArenaOperationalStatus.RESERVED, reservationId, now, ""));
            return ArenaAllocationResult.success(arena, reservation);
        } finally {
            lockRepository.releaseArenaLock(arena.arenaId(), lockToken.get());
        }
    }

    private void sanitizeOrRejectBusyArena(ArenaId arenaId, ArenaOperationalState state) {
        if (state.status() == ArenaOperationalStatus.RESERVED && !state.reservationId().isBlank()) {
            Optional<ArenaReservation> reservation = reservationRepository.findByArenaId(arenaId);
            if (reservation.isPresent() && reservation.get().expiresAt().isBefore(Instant.now(clock))) {
                reservationRepository.remove(arenaId, reservation.get().reservationId());
                operationalStateRepository.save(ArenaOperationalState.available(arenaId, Instant.now(clock)));
            }
        }
    }

    private boolean isProposalStale(ArenaAllocationRequest request) {
        return playerStateService.findCurrentState(request.proposal().leftPlayerId())
                .map(state -> state.status() != PlayerStatus.IN_QUEUE)
                .orElse(true)
                || playerStateService.findCurrentState(request.proposal().rightPlayerId())
                .map(state -> state.status() != PlayerStatus.IN_QUEUE)
                .orElse(true);
    }
}
