package com.stephanofer.prismapractice.api.rating;

public enum RatingApplyFailureReason {
    MATCH_NOT_ELIGIBLE,
    RATINGS_NOT_FOUND,
    INVALID_WINNER_LOSER,
    RANK_TIERS_MISSING,
    SEASON_CONTEXT_MISSING,
    ALREADY_APPLIED,
    PERSISTENCE_FAILURE
}
