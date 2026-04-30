package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.state.PlayerState;

import java.util.Objects;

public final class QueueEligibilityPolicy {

    public QueueJoinFailureReason validate(QueueDefinition queueDefinition, PlayerState playerState, boolean inParty) {
        Objects.requireNonNull(queueDefinition, "queueDefinition");
        if (!queueDefinition.enabled()) {
            return QueueJoinFailureReason.QUEUE_DISABLED;
        }
        if (playerState == null) {
            return QueueJoinFailureReason.PLAYER_STATE_MISSING;
        }
        if (!queueDefinition.allowedStatuses().isEmpty() && !queueDefinition.allowedStatuses().contains(playerState.status())) {
            return QueueJoinFailureReason.BLOCKED_BY_STATE;
        }
        if (queueDefinition.blockedStatuses().contains(playerState.status())) {
            return QueueJoinFailureReason.BLOCKED_BY_STATE;
        }
        if (queueDefinition.blockedIfInParty() && inParty) {
            return QueueJoinFailureReason.BLOCKED_BY_PARTY;
        }
        return null;
    }
}
