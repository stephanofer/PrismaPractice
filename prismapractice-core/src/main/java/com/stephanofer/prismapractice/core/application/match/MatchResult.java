package com.stephanofer.prismapractice.core.application.match;

import com.stephanofer.prismapractice.api.match.MatchSession;

public record MatchResult(boolean success, MatchSession session, MatchCreateFailureReason createFailureReason) {

    public static MatchResult success(MatchSession session) {
        return new MatchResult(true, session, null);
    }

    public static MatchResult failure(MatchCreateFailureReason reason) {
        return new MatchResult(false, null, reason);
    }
}
