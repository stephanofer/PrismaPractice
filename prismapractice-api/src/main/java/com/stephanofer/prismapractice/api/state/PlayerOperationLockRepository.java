package com.stephanofer.prismapractice.api.state;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;

import java.util.Optional;

public interface PlayerOperationLockRepository {

    Optional<String> acquireTransitionLock(PlayerId playerId);

    boolean releaseTransitionLock(PlayerId playerId, String token);

    Optional<String> acquireMatchmakingLock(QueueId queueId, PlayerId playerId);

    boolean releaseMatchmakingLock(QueueId queueId, PlayerId playerId, String token);

    Optional<String> acquireArenaLock(ArenaId arenaId);

    boolean releaseArenaLock(ArenaId arenaId, String token);
}
