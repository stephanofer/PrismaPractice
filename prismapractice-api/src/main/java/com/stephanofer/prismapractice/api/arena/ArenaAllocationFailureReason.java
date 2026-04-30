package com.stephanofer.prismapractice.api.arena;

public enum ArenaAllocationFailureReason {
    NO_COMPATIBLE_ARENA,
    NO_AVAILABLE_ARENA,
    ARENA_ALREADY_RESERVED,
    RESERVATION_CONFLICT,
    ARENA_BROKEN,
    ARENA_DISABLED,
    PROPOSAL_STALE
}
