package com.stephanofer.prismapractice.feedback;

public record TitleFeedbackDelivery(
        String title,
        String subtitle,
        TitleTimes times
) implements FeedbackDelivery {

    @Override
    public FeedbackChannel channel() {
        return FeedbackChannel.TITLE;
    }
}
