package com.stephanofer.prismapractice.paper.scoreboard;

public record PaperScoreboardSettings(
        int tickInterval,
        int defaultRefreshTicks,
        boolean hideWhenDisabledInSettings,
        boolean allowPlaceholderApi
) {

    public PaperScoreboardSettings {
        if (tickInterval < 1) {
            throw new IllegalArgumentException("tickInterval must be >= 1");
        }
        if (defaultRefreshTicks < 1) {
            throw new IllegalArgumentException("defaultRefreshTicks must be >= 1");
        }
    }
}
