package com.stephanofer.prismapractice.api.history;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchSide;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MatchHistoryPlayerSnapshot(
        PlayerId playerId,
        String playerName,
        MatchSide side,
        boolean won,
        Integer srBefore,
        Integer srAfter,
        Integer pingFinal,
        Double finalHealth,
        InventorySnapshot inventorySnapshot,
        Map<String, Integer> remainingConsumables,
        List<String> activeEffects
) {

    public MatchHistoryPlayerSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(inventorySnapshot, "inventorySnapshot");
        Objects.requireNonNull(remainingConsumables, "remainingConsumables");
        Objects.requireNonNull(activeEffects, "activeEffects");
        remainingConsumables = Map.copyOf(remainingConsumables);
        activeEffects = List.copyOf(activeEffects);
        if (playerName.isBlank()) {
            throw new IllegalArgumentException("playerName must not be blank");
        }
    }
}
