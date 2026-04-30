package com.stephanofer.prismapractice.api.matchmaking;

import java.util.Objects;

public record RegionPing(RegionId regionId, int pingMillis) {

    public RegionPing {
        Objects.requireNonNull(regionId, "regionId");
        if (pingMillis < 0) {
            throw new IllegalArgumentException("pingMillis must be >= 0");
        }
    }
}
