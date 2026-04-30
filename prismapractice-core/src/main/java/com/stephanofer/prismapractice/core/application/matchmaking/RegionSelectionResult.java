package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.matchmaking.RegionId;

public record RegionSelectionResult(RegionId regionId, int combinedPing, int maxPing, int pingDifference) {
}
