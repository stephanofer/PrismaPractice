package com.stephanofer.prismapractice.api.rating;

public record RatingApplyResult(boolean success, RatingChange winnerChange, RatingChange loserChange, RatingApplyFailureReason failureReason) {

    public static RatingApplyResult success(RatingChange winnerChange, RatingChange loserChange) {
        return new RatingApplyResult(true, winnerChange, loserChange, null);
    }

    public static RatingApplyResult failure(RatingApplyFailureReason failureReason) {
        return new RatingApplyResult(false, null, null, failureReason);
    }
}
