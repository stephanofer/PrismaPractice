package com.stephanofer.prismapractice.feedback;

public record FeedbackPersistence(
        String slot,
        int intervalTicks,
        int priority
) {
}
