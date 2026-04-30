package com.stephanofer.prismapractice.core.application.queue;

public enum QueueLeaveFailureReason {
    CONCURRENT_MODIFICATION,
    NOT_IN_QUEUE,
    STATE_TRANSITION_FAILED
}
