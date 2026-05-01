package com.stephanofer.prismapractice.debug;

import java.time.Instant;
import java.util.Map;

public record DebugEvent(
        long sequence,
        Instant timestamp,
        String runtime,
        String category,
        String name,
        DebugSeverity severity,
        DebugDetailLevel detailLevel,
        String message,
        Map<String, String> fields,
        String throwableSummary,
        boolean watchTriggered
) {
}
