package com.stephanofer.prismapractice.api.rating;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.List;
import java.util.Optional;

public interface ModeRatingRepository {

    Optional<ModeRating> find(PlayerId playerId, ModeId modeId, String seasonId);

    List<ModeRating> findByPlayerId(PlayerId playerId, String seasonId);

    List<ModeRating> findByModeId(ModeId modeId, String seasonId);

    ModeRating save(ModeRating modeRating);
}
