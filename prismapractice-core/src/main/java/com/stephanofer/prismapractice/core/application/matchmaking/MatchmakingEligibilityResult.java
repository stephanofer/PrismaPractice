package com.stephanofer.prismapractice.core.application.matchmaking;

public record MatchmakingEligibilityResult(boolean eligible, MatchmakingHardFailureReason failureReason) {

    public static MatchmakingEligibilityResult allow() {
        return new MatchmakingEligibilityResult(true, null);
    }

    public static MatchmakingEligibilityResult deny(MatchmakingHardFailureReason reason) {
        return new MatchmakingEligibilityResult(false, reason);
    }
}
