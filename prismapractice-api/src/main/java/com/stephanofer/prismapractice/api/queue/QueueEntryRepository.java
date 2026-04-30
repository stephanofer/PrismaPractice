package com.stephanofer.prismapractice.api.queue;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;

import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository {

    Optional<QueueEntry> findByPlayerId(PlayerId playerId);

    List<QueueEntry> findByQueueId(QueueId queueId);

    QueueEntry save(QueueEntry entry);

    boolean removeByPlayerId(PlayerId playerId);

    boolean remove(QueueId queueId, PlayerId playerId);
}
