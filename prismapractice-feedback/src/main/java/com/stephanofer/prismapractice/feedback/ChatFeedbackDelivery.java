package com.stephanofer.prismapractice.feedback;

import java.util.List;

public record ChatFeedbackDelivery(List<String> lines) implements FeedbackDelivery {

    @Override
    public FeedbackChannel channel() {
        return FeedbackChannel.CHAT;
    }
}
