package com.stephanofer.prismapractice.core.application.matchmaking;

public enum MatchmakingHardFailureReason {
    SAME_PLAYER,
    DIFFERENT_QUEUE,
    PLAYER_NOT_IN_QUEUE,
    STATE_NOT_QUEUE,
    SNAPSHOT_MISSING_REGION_DATA,
    NO_COMMON_REGION,
    PING_RANGE_INCOMPATIBLE,
    SKILL_DELTA_TOO_HIGH
}
