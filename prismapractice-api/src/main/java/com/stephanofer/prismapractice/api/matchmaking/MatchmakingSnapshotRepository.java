package com.stephanofer.prismapractice.api.matchmaking;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;

import java.util.List;
import java.util.Optional;

public interface MatchmakingSnapshotRepository {

    Optional<MatchmakingSnapshot> findByPlayerId(PlayerId playerId);

    List<MatchmakingSnapshot> findByQueueId(QueueId queueId);

    MatchmakingSnapshot save(MatchmakingSnapshot snapshot);

    boolean removeByPlayerId(PlayerId playerId);

    boolean remove(QueueId queueId, PlayerId playerId);
}
