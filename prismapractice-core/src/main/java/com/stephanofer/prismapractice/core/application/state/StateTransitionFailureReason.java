package com.stephanofer.prismapractice.core.application.state;

public enum StateTransitionFailureReason {
    CONCURRENT_TRANSITION,
    STATE_MISSING,
    INVALID_TRANSITION
}
