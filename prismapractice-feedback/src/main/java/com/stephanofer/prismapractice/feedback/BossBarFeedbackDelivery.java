package com.stephanofer.prismapractice.feedback;

import java.util.Set;

public record BossBarFeedbackDelivery(
        FeedbackDeliveryMode mode,
        String message,
        float progress,
        FeedbackBossBarColor color,
        FeedbackBossBarOverlay overlay,
        Set<FeedbackBossBarFlag> flags,
        FeedbackPersistence persistence,
        int durationTicks
) implements FeedbackDelivery {

    @Override
    public FeedbackChannel channel() {
        return FeedbackChannel.BOSSBAR;
    }
}
