package com.stephanofer.prismapractice.api.history;

import java.util.Map;
import java.util.Objects;

public record InventorySnapshot(
        Map<Integer, ItemSnapshot> slotItems,
        Map<String, ItemSnapshot> armorItems,
        ItemSnapshot offhandItem
) {

    public InventorySnapshot {
        Objects.requireNonNull(slotItems, "slotItems");
        Objects.requireNonNull(armorItems, "armorItems");
        slotItems = Map.copyOf(slotItems);
        armorItems = Map.copyOf(armorItems);
    }

    public static InventorySnapshot empty() {
        return new InventorySnapshot(Map.of(), Map.of(), null);
    }
}
