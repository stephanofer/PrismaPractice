package com.stephanofer.prismapractice.api.arena;

public record ArenaAllocationResult(
        boolean success,
        ArenaDefinition arena,
        ArenaReservation reservation,
        ArenaAllocationFailureReason failureReason
) {

    public static ArenaAllocationResult success(ArenaDefinition arena, ArenaReservation reservation) {
        return new ArenaAllocationResult(true, arena, reservation, null);
    }

    public static ArenaAllocationResult failure(ArenaAllocationFailureReason failureReason) {
        return new ArenaAllocationResult(false, null, null, failureReason);
    }
}
