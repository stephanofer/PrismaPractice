package com.stephanofer.prismapractice.core.application.queue;

import com.stephanofer.prismapractice.api.matchmaking.RegionPing;

import java.util.List;
import java.util.Objects;

public record QueueJoinContext(String sourceServerId, List<RegionPing> regionPings) {

    public QueueJoinContext {
        Objects.requireNonNull(sourceServerId, "sourceServerId");
        Objects.requireNonNull(regionPings, "regionPings");
        regionPings = List.copyOf(regionPings);
    }
}
