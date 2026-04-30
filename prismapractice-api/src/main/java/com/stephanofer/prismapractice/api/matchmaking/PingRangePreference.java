package com.stephanofer.prismapractice.api.matchmaking;

public enum PingRangePreference {
    WITHIN_25(25),
    WITHIN_50(50),
    WITHIN_75(75),
    WITHIN_100(100),
    WITHIN_125(125),
    WITHIN_150(150),
    WITHIN_200(200),
    WITHIN_250(250),
    NO_LIMIT(Integer.MAX_VALUE);

    private final int maxDifferenceMillis;

    PingRangePreference(int maxDifferenceMillis) {
        this.maxDifferenceMillis = maxDifferenceMillis;
    }

    public int maxDifferenceMillis() {
        return maxDifferenceMillis;
    }
}
