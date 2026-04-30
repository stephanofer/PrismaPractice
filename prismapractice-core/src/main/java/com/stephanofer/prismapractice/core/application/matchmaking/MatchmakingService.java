package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingProposal;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MatchmakingService {

    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final MatchmakingSnapshotRepository snapshotRepository;
    private final PlayerStateService playerStateService;
    private final PlayerOperationLockRepository lockRepository;
    private final SearchWindowPolicy searchWindowPolicy;
    private final RegionSelectionPolicy regionSelectionPolicy;
    private final MatchQualityScorer qualityScorer;
    private final MatchmakingEligibilityPolicy eligibilityPolicy;
    private final Clock clock;

    public MatchmakingService(
            QueueRepository queueRepository,
            QueueEntryRepository queueEntryRepository,
            MatchmakingSnapshotRepository snapshotRepository,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            Clock clock
    ) {
        this(queueRepository, queueEntryRepository, snapshotRepository, playerStateService, lockRepository,
                new SearchWindowPolicy(), new RegionSelectionPolicy(), new MatchQualityScorer(), new MatchmakingEligibilityPolicy(), clock);
    }

    public MatchmakingService(
            QueueRepository queueRepository,
            QueueEntryRepository queueEntryRepository,
            MatchmakingSnapshotRepository snapshotRepository,
            PlayerStateService playerStateService,
            PlayerOperationLockRepository lockRepository,
            SearchWindowPolicy searchWindowPolicy,
            RegionSelectionPolicy regionSelectionPolicy,
            MatchQualityScorer qualityScorer,
            MatchmakingEligibilityPolicy eligibilityPolicy,
            Clock clock
    ) {
        this.queueRepository = Objects.requireNonNull(queueRepository, "queueRepository");
        this.queueEntryRepository = Objects.requireNonNull(queueEntryRepository, "queueEntryRepository");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository");
        this.playerStateService = Objects.requireNonNull(playerStateService, "playerStateService");
        this.lockRepository = Objects.requireNonNull(lockRepository, "lockRepository");
        this.searchWindowPolicy = Objects.requireNonNull(searchWindowPolicy, "searchWindowPolicy");
        this.regionSelectionPolicy = Objects.requireNonNull(regionSelectionPolicy, "regionSelectionPolicy");
        this.qualityScorer = Objects.requireNonNull(qualityScorer, "qualityScorer");
        this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy, "eligibilityPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<MatchmakingProposal> findBestProposal(QueueId queueId) {
        Objects.requireNonNull(queueId, "queueId");
        QueueDefinition queueDefinition = queueRepository.findById(queueId).orElse(null);
        if (queueDefinition == null || !queueDefinition.enabled()) {
            return Optional.empty();
        }

        List<MatchmakingSnapshot> snapshots = snapshotRepository.findByQueueId(queueId).stream()
                .sorted(Comparator.comparing(MatchmakingSnapshot::joinedAt))
                .toList();
        MatchCandidate bestCandidate = null;

        for (int leftIndex = 0; leftIndex < snapshots.size(); leftIndex++) {
            MatchmakingSnapshot left = snapshots.get(leftIndex);
            if (isSnapshotStale(left, queueId)) {
                sanitizeStalePlayer(left.playerId(), queueId);
                continue;
            }
            for (int rightIndex = leftIndex + 1; rightIndex < snapshots.size(); rightIndex++) {
                MatchmakingSnapshot right = snapshots.get(rightIndex);
                if (isSnapshotStale(right, queueId)) {
                    sanitizeStalePlayer(right.playerId(), queueId);
                    continue;
                }

                MatchCandidate candidate = evaluateCandidate(queueDefinition, left, right);
                if (candidate == null) {
                    continue;
                }
                if (bestCandidate == null || candidate.qualityScore() > bestCandidate.qualityScore()) {
                    bestCandidate = candidate;
                }
            }
        }

        if (bestCandidate == null) {
            return Optional.empty();
        }

        Optional<String> leftToken = lockRepository.acquireMatchmakingLock(queueId, bestCandidate.left().playerId());
        if (leftToken.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> rightToken = lockRepository.acquireMatchmakingLock(queueId, bestCandidate.right().playerId());
        if (rightToken.isEmpty()) {
            lockRepository.releaseMatchmakingLock(queueId, bestCandidate.left().playerId(), leftToken.get());
            return Optional.empty();
        }

        MatchCandidate revalidated = evaluateCandidate(queueDefinition,
                snapshotRepository.findByPlayerId(bestCandidate.left().playerId()).orElse(null),
                snapshotRepository.findByPlayerId(bestCandidate.right().playerId()).orElse(null));
        if (revalidated == null) {
            lockRepository.releaseMatchmakingLock(queueId, bestCandidate.left().playerId(), leftToken.get());
            lockRepository.releaseMatchmakingLock(queueId, bestCandidate.right().playerId(), rightToken.get());
            return Optional.empty();
        }

        return Optional.of(new MatchmakingProposal(
                queueId,
                revalidated.left().playerId(),
                revalidated.right().playerId(),
                revalidated.region().regionId(),
                revalidated.qualityScore(),
                revalidated.searchWindow(),
                "profile=" + queueDefinition.matchmakingProfile().name()
                        + ", window=" + revalidated.searchWindow().key()
                        + ", region=" + revalidated.region().regionId().value()
                        + ", maxPing=" + revalidated.region().maxPing()
                        + ", pingDiff=" + revalidated.region().pingDifference(),
                leftToken.get(),
                rightToken.get()
        ));
    }

    public void releaseProposalLocks(MatchmakingProposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        lockRepository.releaseMatchmakingLock(proposal.queueId(), proposal.leftPlayerId(), proposal.leftLockToken());
        lockRepository.releaseMatchmakingLock(proposal.queueId(), proposal.rightPlayerId(), proposal.rightLockToken());
    }

    private MatchCandidate evaluateCandidate(QueueDefinition queueDefinition, MatchmakingSnapshot left, MatchmakingSnapshot right) {
        if (left == null || right == null) {
            return null;
        }
        PlayerState leftState = playerStateService.findCurrentState(left.playerId()).orElse(null);
        PlayerState rightState = playerStateService.findCurrentState(right.playerId()).orElse(null);
        Duration olderWait = olderWait(left, right);
        var window = searchWindowPolicy.resolve(queueDefinition.searchExpansionStrategy(), olderWait);
        var region = regionSelectionPolicy.selectBestRegion(left, right).orElse(null);
        MatchmakingEligibilityResult eligibility = eligibilityPolicy.validate(queueDefinition, left, right, leftState, rightState, window, region);
        if (!eligibility.eligible()) {
            return null;
        }
        int qualityScore = qualityScorer.score(queueDefinition.matchmakingProfile(), left, right, window, region, olderWait);
        if (qualityScore < window.minimumQualityScore()) {
            return null;
        }
        return new MatchCandidate(left, right, window, region, qualityScore);
    }

    private boolean isSnapshotStale(MatchmakingSnapshot snapshot, QueueId queueId) {
        if (snapshot == null || !snapshot.queueId().equals(queueId)) {
            return true;
        }
        if (queueEntryRepository.findByPlayerId(snapshot.playerId()).isEmpty()) {
            return true;
        }
        return playerStateService.findCurrentState(snapshot.playerId())
                .map(state -> state.status() != com.stephanofer.prismapractice.api.state.PlayerStatus.IN_QUEUE)
                .orElse(true);
    }

    private void sanitizeStalePlayer(PlayerId playerId, QueueId queueId) {
        snapshotRepository.remove(queueId, playerId);
        queueEntryRepository.remove(queueId, playerId);
        playerStateService.findCurrentState(playerId).ifPresent(state -> {
            if (state.status() == PlayerStatus.IN_QUEUE) {
                playerStateService.transition(playerId, PlayerStatus.HUB, PlayerSubStatus.NONE);
            }
        });
    }

    private Duration olderWait(MatchmakingSnapshot left, MatchmakingSnapshot right) {
        Instant olderJoin = left.joinedAt().isBefore(right.joinedAt()) ? left.joinedAt() : right.joinedAt();
        return Duration.between(olderJoin, Instant.now(clock));
    }

    private record MatchCandidate(
            MatchmakingSnapshot left,
            MatchmakingSnapshot right,
            com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow searchWindow,
            RegionSelectionResult region,
            int qualityScore
    ) {
    }
}
