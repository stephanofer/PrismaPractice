package com.stephanofer.prismapractice.hub.hotbar;

import java.util.Map;
import java.util.Objects;

public record HubHotbarConfig(Map<String, HubHotbarProfileConfig> profiles) {

    public HubHotbarConfig {
        Objects.requireNonNull(profiles, "profiles");
        profiles = Map.copyOf(profiles);
    }
}
