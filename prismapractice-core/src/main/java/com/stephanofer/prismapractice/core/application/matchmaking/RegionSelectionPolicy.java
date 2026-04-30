package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.matchmaking.RegionPing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RegionSelectionPolicy {

    public Optional<RegionSelectionResult> selectBestRegion(MatchmakingSnapshot left, MatchmakingSnapshot right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");

        Map<RegionId, Integer> rightPings = new HashMap<>();
        for (RegionPing regionPing : right.regionPings()) {
            rightPings.put(regionPing.regionId(), regionPing.pingMillis());
        }

        return left.regionPings().stream()
                .filter(leftPing -> rightPings.containsKey(leftPing.regionId()))
                .map(leftPing -> {
                    int rightPing = rightPings.get(leftPing.regionId());
                    int combinedPing = leftPing.pingMillis() + rightPing;
                    int maxPing = Math.max(leftPing.pingMillis(), rightPing);
                    int pingDifference = Math.abs(leftPing.pingMillis() - rightPing);
                    return new RegionSelectionResult(leftPing.regionId(), combinedPing, maxPing, pingDifference);
                })
                .min(Comparator.comparingInt(RegionSelectionResult::maxPing)
                        .thenComparingInt(RegionSelectionResult::pingDifference)
                        .thenComparingInt(RegionSelectionResult::combinedPing)
                        .thenComparing(result -> result.regionId().value()));
    }
}
