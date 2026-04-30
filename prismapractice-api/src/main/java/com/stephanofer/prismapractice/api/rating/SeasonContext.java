package com.stephanofer.prismapractice.api.rating;

import java.time.Instant;
import java.util.Objects;

public record SeasonContext(String seasonId, String displayName, String status, Instant startedAt, Instant endedAt) {

    public SeasonContext {
        Objects.requireNonNull(seasonId, "seasonId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}
