package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.core.application.queue.QueueJoinFailureReason;
import com.stephanofer.prismapractice.core.application.queue.QueueLeaveFailureReason;
import com.stephanofer.prismapractice.core.application.queue.QueueLeaveResult;
import com.stephanofer.prismapractice.core.application.queue.QueueJoinResult;
import com.stephanofer.prismapractice.core.application.queue.QueueService;

import java.util.Objects;
import java.util.Optional;

final class HubQueueMenuService {

    private static final String FALLBACK_SOURCE_SERVER_ID = "hub";

    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PlayerPresenceRepository playerPresenceRepository;
    private final QueueService queueService;

    HubQueueMenuService(
            QueueRepository queueRepository,
            QueueEntryRepository queueEntryRepository,
            PlayerPresenceRepository playerPresenceRepository,
            QueueService queueService
    ) {
        this.queueRepository = Objects.requireNonNull(queueRepository, "queueRepository");
        this.queueEntryRepository = Objects.requireNonNull(queueEntryRepository, "queueEntryRepository");
        this.playerPresenceRepository = Objects.requireNonNull(playerPresenceRepository, "playerPresenceRepository");
        this.queueService = Objects.requireNonNull(queueService, "queueService");
    }

    QueueMenuView view(PlayerId playerId, QueueId targetQueueId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(targetQueueId, "targetQueueId");

        Optional<QueueDefinition> definition = queueRepository.findById(targetQueueId);
        Optional<QueueEntry> activeEntry = queueEntryRepository.findByPlayerId(playerId);
        String activeQueueName = activeEntry.map(this::displayName).orElse("");
        int playerCount = definition.map(value -> queueEntryRepository.findByQueueId(value.queueId()).size()).orElse(0);

        if (definition.isEmpty()) {
            return new QueueMenuView(targetQueueId, null, QueueMenuState.MISSING, activeEntry.orElse(null), activeQueueName, playerCount);
        }
        if (!definition.get().enabled()) {
            return new QueueMenuView(targetQueueId, definition.get(), QueueMenuState.DISABLED, activeEntry.orElse(null), activeQueueName, playerCount);
        }
        if (activeEntry.isPresent()) {
            if (activeEntry.get().queueId().equals(targetQueueId)) {
                return new QueueMenuView(targetQueueId, definition.get(), QueueMenuState.CURRENT_QUEUE, activeEntry.get(), activeQueueName, playerCount);
            }
            return new QueueMenuView(targetQueueId, definition.get(), QueueMenuState.OTHER_QUEUE, activeEntry.get(), activeQueueName, playerCount);
        }
        return new QueueMenuView(targetQueueId, definition.get(), QueueMenuState.AVAILABLE, null, "", playerCount);
    }

    QueueMenuActionResult join(PlayerId playerId, QueueId targetQueueId) {
        return click(playerId, targetQueueId);
    }

    QueueMenuActionResult click(PlayerId playerId, QueueId targetQueueId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(targetQueueId, "targetQueueId");

        QueueMenuView view = view(playerId, targetQueueId);
        if (view.state() == QueueMenuState.DISABLED || view.state() == QueueMenuState.MISSING) {
            return QueueMenuActionResult.failure(view, failureReasonFor(view.state()));
        }

        if (view.state() == QueueMenuState.CURRENT_QUEUE) {
            QueueLeaveResult leaveResult = queueService.leaveQueue(playerId);
            if (!leaveResult.success()) {
                return QueueMenuActionResult.failure(view(playerId, targetQueueId), mapLeaveFailure(leaveResult.failureReason()));
            }
            return QueueMenuActionResult.success(QueueMenuAction.LEFT_CURRENT, view(playerId, targetQueueId), null);
        }

        if (view.state() == QueueMenuState.OTHER_QUEUE) {
            QueueLeaveResult leaveResult = queueService.leaveQueue(playerId);
            if (!leaveResult.success() && leaveResult.failureReason() != QueueLeaveFailureReason.NOT_IN_QUEUE) {
                return QueueMenuActionResult.failure(view(playerId, targetQueueId), mapLeaveFailure(leaveResult.failureReason()));
            }
        }

        String sourceServerId = playerPresenceRepository.find(playerId)
                .map(presence -> presence.serverId().isBlank() ? FALLBACK_SOURCE_SERVER_ID : presence.serverId())
                .orElse(FALLBACK_SOURCE_SERVER_ID);

        QueueJoinResult result = queueService.joinQueue(playerId, targetQueueId, sourceServerId);
        QueueMenuView after = view(playerId, targetQueueId);
        if (result.success()) {
            return QueueMenuActionResult.success(view.state() == QueueMenuState.OTHER_QUEUE ? QueueMenuAction.SWITCHED : QueueMenuAction.JOINED, after, result.entry());
        }
        return QueueMenuActionResult.failure(after, result.failureReason());
    }

    private String displayName(QueueEntry entry) {
        return queueRepository.findById(entry.queueId())
                .map(QueueDefinition::displayName)
                .orElse(entry.queueId().toString());
    }

    private QueueJoinFailureReason failureReasonFor(QueueMenuState state) {
        return switch (state) {
            case CURRENT_QUEUE -> QueueJoinFailureReason.PLAYER_ALREADY_IN_QUEUE;
            case OTHER_QUEUE -> QueueJoinFailureReason.PLAYER_ALREADY_IN_OTHER_QUEUE;
            case DISABLED -> QueueJoinFailureReason.QUEUE_DISABLED;
            case MISSING -> QueueJoinFailureReason.QUEUE_NOT_FOUND;
            case AVAILABLE -> throw new IllegalArgumentException("AVAILABLE state has no synthetic failure reason");
        };
    }

    private QueueJoinFailureReason mapLeaveFailure(QueueLeaveFailureReason failureReason) {
        return switch (failureReason) {
            case CONCURRENT_MODIFICATION -> QueueJoinFailureReason.CONCURRENT_MODIFICATION;
            case NOT_IN_QUEUE, STATE_TRANSITION_FAILED -> QueueJoinFailureReason.STATE_TRANSITION_FAILED;
        };
    }
}
