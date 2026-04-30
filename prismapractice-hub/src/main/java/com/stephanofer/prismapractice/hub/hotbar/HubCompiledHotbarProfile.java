package com.stephanofer.prismapractice.hub.hotbar;

import java.util.Map;
import java.util.Objects;

record HubCompiledHotbarProfile(
        String key,
        int selectedSlot,
        boolean resetInventory,
        HubHotbarConstraints constraints,
        Map<Integer, HubCompiledItem> items
) {

    HubCompiledHotbarProfile {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(items, "items");
        items = Map.copyOf(items);
    }
}
