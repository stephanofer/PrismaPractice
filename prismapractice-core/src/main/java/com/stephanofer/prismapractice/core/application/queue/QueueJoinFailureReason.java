package com.stephanofer.prismapractice.core.application.queue;

public enum QueueJoinFailureReason {
    CONCURRENT_MODIFICATION,
    QUEUE_NOT_FOUND,
    QUEUE_DISABLED,
    PLAYER_STATE_MISSING,
    PLAYER_ALREADY_IN_QUEUE,
    PLAYER_ALREADY_IN_OTHER_QUEUE,
    BLOCKED_BY_STATE,
    BLOCKED_BY_PARTY,
    STATE_TRANSITION_FAILED
}
