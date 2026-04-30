package com.stephanofer.prismapractice.feedback;

public record SoundFeedbackDelivery(
        String sound,
        FeedbackSoundSource source,
        float volume,
        float pitch
) implements FeedbackDelivery {

    @Override
    public FeedbackChannel channel() {
        return FeedbackChannel.SOUND;
    }
}
