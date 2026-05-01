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
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugContext;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;

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
    private final DebugController debug;

    public PlayerStateService(
            PlayerStateRepository stateRepository,
            PlayerPresenceRepository presenceRepository,
            PlayerOperationLockRepository lockRepository,
            Clock clock
    ) {
        this(stateRepository, presenceRepository, lockRepository, new PlayerStateTransitionPolicy(), clock, DebugController.noop());
    }

    public PlayerStateService(
            PlayerStateRepository stateRepository,
            PlayerPresenceRepository presenceRepository,
            PlayerOperationLockRepository lockRepository,
            PlayerStateTransitionPolicy transitionPolicy,
            Clock clock
    ) {
        this(stateRepository, presenceRepository, lockRepository, transitionPolicy, clock, DebugController.noop());
    }

    public PlayerStateService(
            PlayerStateRepository stateRepository,
            PlayerPresenceRepository presenceRepository,
            PlayerOperationLockRepository lockRepository,
            PlayerStateTransitionPolicy transitionPolicy,
            Clock clock,
            DebugController debug
    ) {
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.presenceRepository = Objects.requireNonNull(presenceRepository, "presenceRepository");
        this.lockRepository = Objects.requireNonNull(lockRepository, "lockRepository");
        this.transitionPolicy = Objects.requireNonNull(transitionPolicy, "transitionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.debug = Objects.requireNonNull(debug, "debug");
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
        debug.debug(DebugCategories.PLAYER_STATE, DebugDetailLevel.BASIC, "state.hub.ensure.requested", "Ensuring online hub state", baseContext(playerId).field("server", serverId).field("runtimeType", runtimeType).build());
        return withTransitionLock(playerId, () -> {
            Instant now = Instant.now(clock);
            presenceRepository.save(PracticePresence.online(playerId, runtimeType, serverId, now));
            PlayerState current = stateRepository.find(playerId).orElse(null);
            if (current == null || current.status() == PlayerStatus.OFFLINE) {
                StateTransitionResult result = StateTransitionResult.success(stateRepository.save(PlayerState.hub(playerId, now)));
                logTransitionResult("state.hub.ensure.completed", current, result, playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
                return result;
            }
            if (current.status() == PlayerStatus.HUB) {
                StateTransitionResult result = StateTransitionResult.success(stateRepository.save(current.withStatus(PlayerStatus.HUB, PlayerSubStatus.NONE, now)));
                logTransitionResult("state.hub.ensure.completed", current, result, playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
                return result;
            }
            StateTransitionResult result = StateTransitionResult.success(current);
            logTransitionResult("state.hub.ensure.completed", current, result, playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
            return result;
        });
    }

    public StateTransitionResult markOffline(PlayerId playerId) {
        debug.debug(DebugCategories.PLAYER_STATE, DebugDetailLevel.BASIC, "state.offline.requested", "Marking player offline", baseContext(playerId).build());
        return withTransitionLock(playerId, () -> {
            Instant now = Instant.now(clock);
            presenceRepository.save(PracticePresence.offline(playerId, now));
            StateTransitionResult result = StateTransitionResult.success(stateRepository.save(PlayerState.offline(playerId, now)));
            logTransitionResult("state.offline.completed", null, result, playerId, PlayerStatus.OFFLINE, PlayerSubStatus.NONE);
            return result;
        });
    }

    public StateTransitionResult transition(PlayerId playerId, PlayerStatus targetStatus, PlayerSubStatus targetSubStatus) {
        debug.debug(DebugCategories.PLAYER_STATE, DebugDetailLevel.BASIC, "state.transition.requested", "Player state transition requested", baseContext(playerId).field("targetStatus", targetStatus).field("targetSubStatus", targetSubStatus).build());
        return withTransitionLock(playerId, () -> transitionLocked(playerId, targetStatus, targetSubStatus));
    }

    public StateTransitionResult transitionLocked(PlayerId playerId, PlayerStatus targetStatus, PlayerSubStatus targetSubStatus) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(targetStatus, "targetStatus");
        Objects.requireNonNull(targetSubStatus, "targetSubStatus");
        PlayerState current = stateRepository.find(playerId).orElse(null);
        if (current == null) {
            StateTransitionResult result = StateTransitionResult.failure(null, StateTransitionFailureReason.STATE_MISSING);
            logTransitionResult("state.transition.rejected", null, result, playerId, targetStatus, targetSubStatus);
            return result;
        }
        if (!transitionPolicy.canTransition(current.status(), targetStatus)) {
            StateTransitionResult result = StateTransitionResult.failure(current, StateTransitionFailureReason.INVALID_TRANSITION);
            logTransitionResult("state.transition.rejected", current, result, playerId, targetStatus, targetSubStatus);
            return result;
        }
        PlayerState next = current.withStatus(targetStatus, targetSubStatus, Instant.now(clock));
        StateTransitionResult result = StateTransitionResult.success(stateRepository.save(next));
        logTransitionResult("state.transition.completed", current, result, playerId, targetStatus, targetSubStatus);
        return result;
    }

    private StateTransitionResult withTransitionLock(PlayerId playerId, LockedTransitionWork work) {
        Optional<String> token = lockRepository.acquireTransitionLock(playerId);
        if (token.isEmpty()) {
            StateTransitionResult result = StateTransitionResult.failure(stateRepository.find(playerId).orElse(null), StateTransitionFailureReason.CONCURRENT_TRANSITION);
            logTransitionResult("state.transition.locked", stateRepository.find(playerId).orElse(null), result, playerId, null, null);
            return result;
        }
        try {
            return work.execute();
        } finally {
            lockRepository.releaseTransitionLock(playerId, token.get());
        }
    }

    private void logTransitionResult(String eventName, PlayerState current, StateTransitionResult result, PlayerId playerId, PlayerStatus targetStatus, PlayerSubStatus targetSubStatus) {
        DebugContext.Builder context = baseContext(playerId)
                .field("fromStatus", current == null ? null : current.status())
                .field("fromSubStatus", current == null ? null : current.subStatus())
                .field("targetStatus", targetStatus)
                .field("targetSubStatus", targetSubStatus)
                .field("success", result.success())
                .field("failureReason", result.failureReason())
                .field("resultStatus", result.state() == null ? null : result.state().status())
                .field("resultSubStatus", result.state() == null ? null : result.state().subStatus());
        if (result.success()) {
            debug.debug(DebugCategories.PLAYER_STATE, DebugDetailLevel.BASIC, eventName, "Player state transition applied", context.build());
            return;
        }
        debug.warn(DebugCategories.PLAYER_STATE, eventName, "Player state transition rejected", context.build());
    }

    private DebugContext.Builder baseContext(PlayerId playerId) {
        return debug.context().player(playerId.value().toString(), null);
    }

    @FunctionalInterface
    private interface LockedTransitionWork {
        StateTransitionResult execute();
    }
}
