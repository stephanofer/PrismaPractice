package com.stephanofer.prismapractice.core.application.match;

public enum MatchCreateFailureReason {
    PROPOSAL_STALE,
    RESERVATION_INVALID,
    PLAYER_ALREADY_IN_ACTIVE_MATCH,
    PERSISTENCE_FAILURE,
    ARENA_CONFIRMATION_FAILED
}
