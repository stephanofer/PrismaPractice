package com.stephanofer.prismapractice.api.arena;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;

import java.util.Objects;
import java.util.Set;

public record ArenaDefinition(
        ArenaId arenaId,
        String displayName,
        ArenaType arenaType,
        RegionId regionId,
        RuntimeType runtimeType,
        String serverPoolKey,
        Set<ModeId> allowedModes,
        Set<ModeId> blockedModes,
        Set<PlayerType> allowedPlayerTypes,
        boolean enabled,
        boolean selectable,
        int selectionWeight,
        boolean featured
) {

    public ArenaDefinition {
        Objects.requireNonNull(arenaId, "arenaId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(arenaType, "arenaType");
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(runtimeType, "runtimeType");
        Objects.requireNonNull(serverPoolKey, "serverPoolKey");
        Objects.requireNonNull(allowedModes, "allowedModes");
        Objects.requireNonNull(blockedModes, "blockedModes");
        Objects.requireNonNull(allowedPlayerTypes, "allowedPlayerTypes");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (serverPoolKey.isBlank()) {
            throw new IllegalArgumentException("serverPoolKey must not be blank");
        }
        if (selectionWeight < 0) {
            throw new IllegalArgumentException("selectionWeight must be >= 0");
        }
        allowedModes = Set.copyOf(allowedModes);
        blockedModes = Set.copyOf(blockedModes);
        allowedPlayerTypes = Set.copyOf(allowedPlayerTypes);
    }
}
