package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshot;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;

import java.time.Duration;
import java.util.Objects;

public final class MatchQualityScorer {

    public int score(
            MatchmakingProfile profile,
            MatchmakingSnapshot left,
            MatchmakingSnapshot right,
            MatchmakingSearchWindow window,
            RegionSelectionResult region,
            Duration olderWait
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(olderWait, "olderWait");

        int skillDelta = Math.abs(left.skillValue() - right.skillValue());
        int skillScore = Math.max(0, 100 - Math.min(100, skillDelta * 100 / Math.max(1, window.maxSkillDelta())));
        int connectionScore = Math.max(0, 100 - Math.min(100, region.maxPing() / 3) - Math.min(35, region.pingDifference() / 4));
        int waitScore = Math.min(100, (int) olderWait.toSeconds() * 2);

        return switch (profile) {
            case QUALITY_FIRST -> (skillScore * 50 + connectionScore * 35 + waitScore * 15) / 100;
            case SPEED_FIRST -> (waitScore * 45 + skillScore * 30 + connectionScore * 25) / 100;
        };
    }
}
