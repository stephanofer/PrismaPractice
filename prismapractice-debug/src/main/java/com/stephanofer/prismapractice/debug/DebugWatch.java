package com.stephanofer.prismapractice.debug;

import java.time.Instant;

public record DebugWatch(long id, String type, String subject, DebugDetailLevel level, Instant expiresAt) {
}
