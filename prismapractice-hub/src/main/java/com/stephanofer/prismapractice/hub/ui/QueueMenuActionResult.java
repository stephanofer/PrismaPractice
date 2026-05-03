package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.core.application.queue.QueueJoinFailureReason;

record QueueMenuActionResult(
        QueueMenuAction action,
        boolean success,
        QueueMenuView view,
        QueueEntry joinedEntry,
        QueueJoinFailureReason failureReason
) {

    static QueueMenuActionResult success(QueueMenuAction action, QueueMenuView view, QueueEntry joinedEntry) {
        return new QueueMenuActionResult(action, true, view, joinedEntry, null);
    }

    static QueueMenuActionResult failure(QueueMenuView view, QueueJoinFailureReason failureReason) {
        return new QueueMenuActionResult(QueueMenuAction.FAILED, false, view, null, failureReason);
    }
}
