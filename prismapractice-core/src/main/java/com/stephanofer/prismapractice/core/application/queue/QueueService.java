package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.matchmaking.RegionPing;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.queue.PlayerPartyIndexRepository;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.core.application.state.StateTransitionResult;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class QueueService {

    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final MatchmakingSnapshotRepository snapshotRepository;
    private final ProfileRepository profileRepository;
    private final PlayerPartyIndexRepository playerPartyIndexRepository;
    private final PlayerStateService playerStateService;
    private final PlayerOperationLockRepository lockRepository;
    private final QueueEligibilityPolicy eligibilityPolicy;
    private final Clock clock;

    public QueueService(
            QueueRepository queueRepository,
            QueueEntryRepository queueEntryRepository,
            MatchmakingSnapshotRepository snapshotRepository,
            ProfileRepository profileRepository,
            PlayerPartyIndexRepository playerPartyIndexRepository,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            Clock clock
    ) {
        this(queueRepository, queueEntryRepository, snapshotRepository, profileRepository, playerPartyIndexRepository, playerStateService, lockRepository, new QueueEligibilityPolicy(), clock);
    }

    public QueueService(
            QueueRepository queueRepository,
            QueueEntryRepository queueEntryRepository,
            MatchmakingSnapshotRepository snapshotRepository,
            ProfileRepository profileRepository,
            PlayerPartyIndexRepository playerPartyIndexRepository,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            QueueEligibilityPolicy eligibilityPolicy,
            Clock clock
    ) {
        this.queueRepository = Objects.requireNonNull(queueRepository, "queueRepository");
        this.queueEntryRepository = Objects.requireNonNull(queueEntryRepository, "queueEntryRepository");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.playerPartyIndexRepository = Objects.requireNonNull(playerPartyIndexRepository, "playerPartyIndexRepository");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.lockRepository = Objects.requireNonNull(lockRepository, "lockRepository");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public QueueJoinResult joinQueue(PlayerId playerId, QueueId queueId, String sourceServerId) {
        return joinQueue(playerId, queueId, new QueueJoinContext(sourceServerId, List.of(new RegionPing(new RegionId("global"), 0))));
    }

    public QueueJoinResult joinQueue(PlayerId playerId, QueueId queueId, QueueJoinContext joinContext) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(joinContext, "joinContext");

        Optional<String> transitionToken = lockRepository.acquireTransitionLock(playerId);
        if (transitionToken.isEmpty()) {
            return QueueJoinResult.failure(playerStateService.findCurrentState(playerId).orElse(null), QueueJoinFailureReason.CONCURRENT_MODIFICATION);
        }

        try {
            QueueDefinition queueDefinition = queueRepository.findById(queueId).orElse(null);
            if (queueDefinition == null) {
                return QueueJoinResult.failure(playerStateService.findCurrentState(playerId).orElse(null), QueueJoinFailureReason.QUEUE_NOT_FOUND);
            }

            Optional<String> matchmakingToken = lockRepository.acquireMatchmakingLock(queueId, playerId);
            if (matchmakingToken.isEmpty()) {
                return QueueJoinResult.failure(playerStateService.findCurrentState(playerId).orElse(null), QueueJoinFailureReason.CONCURRENT_MODIFICATION);
            }

            try {
                PlayerState currentState = playerStateService.findCurrentState(playerId).orElse(null);
                Optional<QueueEntry> existingEntry = queueEntryRepository.findByPlayerId(playerId);
                if (existingEntry.isPresent()) {
                    if (currentState != null && currentState.status() == PlayerStatus.IN_QUEUE) {
                        QueueJoinFailureReason reason = existingEntry.get().queueId().equals(queueId)
                                ? QueueJoinFailureReason.PLAYER_ALREADY_IN_QUEUE
                                : QueueJoinFailureReason.PLAYER_ALREADY_IN_OTHER_QUEUE;
                        return QueueJoinResult.failure(currentState, reason);
                    }
                    queueEntryRepository.remove(existingEntry.get().queueId(), playerId);
                    snapshotRepository.remove(existingEntry.get().queueId(), playerId);
                }

                QueueJoinFailureReason eligibilityFailure = eligibilityPolicy.validate(
                        queueDefinition,
                        currentState,
                        playerPartyIndexRepository.isInParty(playerId)
                );
                if (eligibilityFailure != null) {
                    return QueueJoinResult.failure(currentState, eligibilityFailure);
                }

                StateTransitionResult transition = playerStateService.transitionLocked(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH);
                if (!transition.success()) {
                    return QueueJoinResult.failure(transition.state(), QueueJoinFailureReason.STATE_TRANSITION_FAILED);
                }

                QueueEntry entry = queueEntryRepository.save(new QueueEntry(
                        playerId,
                        queueId,
                        Instant.now(clock),
                        queueDefinition.matchmakingProfile(),
                        queueDefinition.queueType(),
                        queueDefinition.playerType(),
                        joinContext.sourceServerId()
                ));
                PracticeProfile profile = profileRepository.findProfile(playerId).orElse(null);
                PracticeSettings settings = profileRepository.findSettings(playerId).orElse(PracticeSettings.defaults(playerId));
                snapshotRepository.save(new MatchmakingSnapshot(
                        playerId,
                        queueId,
                        entry.joinedAt(),
                        PlayerStatus.IN_QUEUE,
                        profile == null ? 0 : profile.currentGlobalRating(),
                        settings.pingRangePreference(),
                        joinContext.regionPings(),
                        joinContext.sourceServerId(),
                        Instant.now(clock)
                ));
                return QueueJoinResult.success(entry, transition.state());
            } finally {
                lockRepository.releaseMatchmakingLock(queueId, playerId, matchmakingToken.get());
            }
        } finally {
            lockRepository.releaseTransitionLock(playerId, transitionToken.get());
        }
    }

    public QueueLeaveResult leaveQueue(PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Optional<String> transitionToken = lockRepository.acquireTransitionLock(playerId);
        if (transitionToken.isEmpty()) {
            return QueueLeaveResult.failure(playerStateService.findCurrentState(playerId).orElse(null), QueueLeaveFailureReason.CONCURRENT_MODIFICATION, false);
        }

        try {
            PlayerState currentState = playerStateService.findCurrentState(playerId).orElse(null);
            Optional<QueueEntry> existingEntry = queueEntryRepository.findByPlayerId(playerId);
            if (existingEntry.isEmpty()) {
                if (currentState != null && currentState.status() == PlayerStatus.IN_QUEUE) {
                    StateTransitionResult repaired = playerStateService.transitionLocked(playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
                    if (!repaired.success()) {
                        return QueueLeaveResult.failure(repaired.state(), QueueLeaveFailureReason.STATE_TRANSITION_FAILED, true);
                    }
                    return QueueLeaveResult.failure(repaired.state(), QueueLeaveFailureReason.NOT_IN_QUEUE, true);
                }
                return QueueLeaveResult.failure(currentState, QueueLeaveFailureReason.NOT_IN_QUEUE, false);
            }

            QueueEntry entry = existingEntry.get();
            Optional<String> matchmakingToken = lockRepository.acquireMatchmakingLock(entry.queueId(), playerId);
            if (matchmakingToken.isEmpty()) {
                return QueueLeaveResult.failure(currentState, QueueLeaveFailureReason.CONCURRENT_MODIFICATION, false);
            }

            try {
                queueEntryRepository.remove(entry.queueId(), playerId);
                snapshotRepository.remove(entry.queueId(), playerId);
                if (currentState != null && currentState.status() == PlayerStatus.IN_QUEUE) {
                    StateTransitionResult transition = playerStateService.transitionLocked(playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
                    if (!transition.success()) {
                        return QueueLeaveResult.failure(transition.state(), QueueLeaveFailureReason.STATE_TRANSITION_FAILED, false);
                    }
                    return QueueLeaveResult.success(entry, transition.state(), false);
                }

                PlayerState repairedState = currentState;
                if (currentState != null && currentState.status() != PlayerStatus.HUB) {
                    repairedState = playerStateService.findCurrentState(playerId).orElse(currentState);
                }
                return QueueLeaveResult.success(entry, repairedState, true);
            } finally {
                lockRepository.releaseMatchmakingLock(entry.queueId(), playerId, matchmakingToken.get());
            }
        } finally {
            lockRepository.releaseTransitionLock(playerId, transitionToken.get());
        }
    }
}
