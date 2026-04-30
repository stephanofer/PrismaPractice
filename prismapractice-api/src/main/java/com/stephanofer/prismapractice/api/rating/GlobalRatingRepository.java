package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Optional;

public interface GlobalRatingRepository {

    Optional<GlobalRatingSnapshot> find(PlayerId playerId, String seasonId);

    java.util.List<GlobalRatingSnapshot> findBySeasonId(String seasonId);

    GlobalRatingSnapshot save(GlobalRatingSnapshot snapshot);
}
