package com.stephanofer.prismapractice.hub.hotbar;

import java.util.Map;
import java.util.Objects;

record HubHotbarProfileConfig(
        String key,
        int selectedSlot,
        boolean resetInventory,
        HubHotbarConstraints constraints,
        Map<Integer, HubHotbarItemConfig> items
) {

    HubHotbarProfileConfig {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(items, "items");
        items = Map.copyOf(items);
    }
}
