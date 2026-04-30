package com.stephanofer.prismapractice.core.application.state;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.api.state.PracticePresence;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PlayerStateService {

    private final PlayerStateRepository stateRepository;
    private final PlayerPresenceRepository presenceRepository;
    private final PlayerOperationLockRepository lockRepository;
    private final PlayerStateTransitionPolicy transitionPolicy;
    private final Clock clock;

    public PlayerStateService(
            PlayerStateRepository stateRepository,
            PlayerPresenceRepository presenceRepository,
            PlayerOperationLockRepository lockRepository,
            Clock clock
    ) {
        this(stateRepository, presenceRepository, lockRepository, new PlayerStateTransitionPolicy(), clock);
    }

    public PlayerStateService(
            PlayerStateRepository stateRepository,
            PlayerPresenceRepository presenceRepository,
            PlayerOperationLockRepository lockRepository,
            PlayerStateTransitionPolicy transitionPolicy,
            Clock clock
    ) {
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.presenceRepository = Objects.requireNonNull(presenceRepository, "presenceRepository");
        this.lockRepository = Objects.requireNonNull(lockRepository, "lockRepository");
        this.transitionPolicy = Objects.requireNonNull(transitionPolicy, "transitionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<PlayerState> findCurrentState(PlayerId playerId) {
        return stateRepository.find(playerId);
    }

    public Optional<PracticePresence> findPresence(PlayerId playerId) {
        return presenceRepository.find(playerId);
    }

    public StateTransitionResult ensureOnlineHubState(PlayerId playerId, String serverId, RuntimeType runtimeType) {
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(runtimeType, "runtimeType");
        return withTransitionLock(playerId, () -> {
            Instant now = Instant.now(clock);
            presenceRepository.save(PracticePresence.online(playerId, runtimeType, serverId, now));
            PlayerState current = stateRepository.find(playerId).orElse(null);
            if (current == null || current.status() == PlayerStatus.OFFLINE) {
                return StateTransitionResult.success(stateRepository.save(PlayerState.hub(playerId, now)));
            }
            if (current.status() == PlayerStatus.HUB) {
                return StateTransitionResult.success(stateRepository.save(current.withStatus(PlayerStatus.HUB, PlayerSubStatus.NONE, now)));
            }
            return StateTransitionResult.success(current);
        });
    }

    public StateTransitionResult markOffline(PlayerId playerId) {
        return withTransitionLock(playerId, () -> {
            Instant now = Instant.now(clock);
            presenceRepository.save(PracticePresence.offline(playerId, now));
            return StateTransitionResult.success(stateRepository.save(PlayerState.offline(playerId, now)));
        });
    }

    public StateTransitionResult transition(PlayerId playerId, PlayerStatus targetStatus, PlayerSubStatus targetSubStatus) {
        return withTransitionLock(playerId, () -> transitionLocked(playerId, targetStatus, targetSubStatus));
    }

    public StateTransitionResult transitionLocked(PlayerId playerId, PlayerStatus targetStatus, PlayerSubStatus targetSubStatus) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(targetStatus, "targetStatus");
        Objects.requireNonNull(targetSubStatus, "targetSubStatus");
        PlayerState current = stateRepository.find(playerId).orElse(null);
        if (current == null) {
            return StateTransitionResult.failure(null, StateTransitionFailureReason.STATE_MISSING);
        }
        if (!transitionPolicy.canTransition(current.status(), targetStatus)) {
            return StateTransitionResult.failure(current, StateTransitionFailureReason.INVALID_TRANSITION);
        }
        PlayerState next = current.withStatus(targetStatus, targetSubStatus, Instant.now(clock));
        return StateTransitionResult.success(stateRepository.save(next));
    }

    private StateTransitionResult withTransitionLock(PlayerId playerId, LockedTransitionWork work) {
        Optional<String> token = lockRepository.acquireTransitionLock(playerId);
        if (token.isEmpty()) {
            return StateTransitionResult.failure(stateRepository.find(playerId).orElse(null), StateTransitionFailureReason.CONCURRENT_TRANSITION);
        }
        try {
            return work.execute();
        } finally {
            lockRepository.releaseTransitionLock(playerId, token.get());
        }
    }

    @FunctionalInterface
    private interface LockedTransitionWork {
        StateTransitionResult execute();
    }
}
