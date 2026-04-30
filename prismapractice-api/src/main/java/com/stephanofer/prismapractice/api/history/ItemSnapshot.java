package com.stephanofer.prismapractice.api.history;

import java.util.Map;
import java.util.Objects;

public record ItemSnapshot(
        String materialKey,
        int amount,
        String displayName,
        String lore,
        Integer customModelData,
        Map<String, Integer> enchants,
        Integer damage
) {

    public ItemSnapshot {
        Objects.requireNonNull(materialKey, "materialKey");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(lore, "lore");
        Objects.requireNonNull(enchants, "enchants");
        enchants = Map.copyOf(enchants);
        if (materialKey.isBlank()) {
            throw new IllegalArgumentException("materialKey must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
    }
}
