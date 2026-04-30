package com.stephanofer.prismapractice.core.application.arena;

import com.stephanofer.prismapractice.api.arena.ArenaAllocationRequest;
import com.stephanofer.prismapractice.api.arena.ArenaDefinition;

import java.util.Objects;

public final class ArenaCompatibilityPolicy {

    public boolean isCompatible(ArenaAllocationRequest request, ArenaDefinition arena) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(arena, "arena");

        if (!arena.enabled() || !arena.selectable()) {
            return false;
        }
        if (arena.arenaType() != request.arenaType()) {
            return false;
        }
        if (arena.runtimeType() != request.targetRuntime()) {
            return false;
        }
        if (!arena.regionId().equals(request.requiredRegion())) {
            return false;
        }
        if (!arena.allowedPlayerTypes().contains(request.playerType())) {
            return false;
        }
        if (!arena.allowedModes().isEmpty() && !arena.allowedModes().contains(request.modeId())) {
            return false;
        }
        return !arena.blockedModes().contains(request.modeId());
    }
}
