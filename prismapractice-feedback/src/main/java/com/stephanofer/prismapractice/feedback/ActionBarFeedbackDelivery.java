package com.stephanofer.prismapractice.feedback;

public record ActionBarFeedbackDelivery(
        FeedbackDeliveryMode mode,
        String message,
        FeedbackPersistence persistence
) implements FeedbackDelivery {

    @Override
    public FeedbackChannel channel() {
        return FeedbackChannel.ACTION_BAR;
    }
}
