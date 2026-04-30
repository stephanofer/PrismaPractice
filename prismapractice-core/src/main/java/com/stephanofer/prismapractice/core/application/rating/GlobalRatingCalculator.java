package com.stephanofer.prismapractice.core.application.rating;

import com.stephanofer.prismapractice.api.rating.ModeRating;

import java.util.List;

public final class GlobalRatingCalculator {

    public int calculate(List<ModeRating> modeRatings) {
        if (modeRatings.isEmpty()) {
            return 1000;
        }
        int total = modeRatings.stream().mapToInt(ModeRating::currentSr).sum();
        return Math.round((float) total / modeRatings.size());
    }
}
