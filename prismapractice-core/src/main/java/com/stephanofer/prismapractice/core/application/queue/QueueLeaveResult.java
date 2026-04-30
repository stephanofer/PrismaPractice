package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.state.PlayerState;

public record QueueLeaveResult(
        boolean success,
        QueueEntry removedEntry,
        PlayerState state,
        QueueLeaveFailureReason failureReason,
        boolean repairedState
) {

    public static QueueLeaveResult success(QueueEntry removedEntry, PlayerState state, boolean repairedState) {
        return new QueueLeaveResult(true, removedEntry, state, null, repairedState);
    }

    public static QueueLeaveResult failure(PlayerState state, QueueLeaveFailureReason failureReason, boolean repairedState) {
        return new QueueLeaveResult(false, null, state, failureReason, repairedState);
    }
}
