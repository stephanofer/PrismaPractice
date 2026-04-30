package com.stephanofer.prismapractice.core.application.history;

public enum HistoryRecordFailureReason {
    MATCH_NOT_COMPLETED,
    MATCH_ALREADY_RECORDED,
    INVALID_MATCH_TIMESTAMPS,
    PLAYER_SNAPSHOTS_INCOMPLETE,
    PLAYER_SNAPSHOTS_INCONSISTENT,
    INVALID_WINNER,
    RECENT_QUERY_INVALID,
    PERSISTENCE_FAILURE
}
