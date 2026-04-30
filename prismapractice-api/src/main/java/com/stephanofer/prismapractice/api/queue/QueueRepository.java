package com.stephanofer.prismapractice.api.queue;

import com.stephanofer.prismapractice.api.common.QueueId;

import java.util.Optional;

public interface QueueRepository {

    Optional<QueueDefinition> findById(QueueId queueId);

    QueueDefinition save(QueueDefinition queueDefinition);
}
