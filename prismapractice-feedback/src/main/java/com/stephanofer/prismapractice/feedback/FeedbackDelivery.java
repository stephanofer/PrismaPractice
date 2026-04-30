package com.stephanofer.prismapractice.feedback;

public sealed interface FeedbackDelivery permits ChatFeedbackDelivery, ActionBarFeedbackDelivery, TitleFeedbackDelivery, SoundFeedbackDelivery, BossBarFeedbackDelivery {

    FeedbackChannel channel();
}
