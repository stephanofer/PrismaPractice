package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.state.PlayerState;

public record QueueJoinResult(boolean success, QueueEntry entry, PlayerState state, QueueJoinFailureReason failureReason) {

    public static QueueJoinResult success(QueueEntry entry, PlayerState state) {
        return new QueueJoinResult(true, entry, state, null);
    }

    public static QueueJoinResult failure(PlayerState state, QueueJoinFailureReason reason) {
        return new QueueJoinResult(false, null, state, reason);
    }
}
