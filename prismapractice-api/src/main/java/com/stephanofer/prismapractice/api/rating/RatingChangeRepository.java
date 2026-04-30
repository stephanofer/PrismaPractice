package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;

public interface RatingChangeRepository {

    boolean exists(MatchId matchId, PlayerId playerId);

    RatingChange save(RatingChange ratingChange);
}
