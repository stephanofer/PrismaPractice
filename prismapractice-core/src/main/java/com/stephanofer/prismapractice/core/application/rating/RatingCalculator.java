package com.stephanofer.prismapractice.core.application.rating;

import com.stephanofer.prismapractice.api.rating.ModeRating;

public final class RatingCalculator {

    public RatingDelta calculateWinnerDelta(ModeRating winner, ModeRating loser) {
        return calculate(winner, loser, 1.0d);
    }

    public RatingDelta calculateLoserDelta(ModeRating loser, ModeRating winner) {
        return calculate(loser, winner, 0.0d);
    }

    private RatingDelta calculate(ModeRating subject, ModeRating opponent, double actualScore) {
        double expected = 1.0d / (1.0d + Math.pow(10.0d, (opponent.currentSr() - subject.currentSr()) / 400.0d));
        int kFactor = subject.placementsCompleted() ? (subject.currentSr() >= 2200 ? 24 : 32) : 48;
        int rawDelta = (int) Math.round(kFactor * (actualScore - expected));
        int clamped = Math.max(-50, Math.min(50, rawDelta));
        if (clamped == 0) {
            clamped = actualScore == 1.0d ? 5 : -5;
        } else if (actualScore == 1.0d) {
            clamped = Math.max(5, clamped);
        } else {
            clamped = Math.min(-5, clamped);
        }
        return new RatingDelta(clamped, kFactor, expected);
    }

    public record RatingDelta(int delta, int kFactor, double expectedScore) {
    }
}
