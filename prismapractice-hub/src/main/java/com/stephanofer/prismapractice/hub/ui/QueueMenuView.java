package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;

record QueueMenuView(
        QueueId targetQueueId,
        QueueDefinition definition,
        QueueMenuState state,
        QueueEntry activeEntry,
        String activeQueueName,
        int playerCount
) {
}
