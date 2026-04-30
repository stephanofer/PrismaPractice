package com.stephanofer.prismapractice.api.rating;

import java.util.List;

public interface RankTierRepository {

    List<RankTier> findEnabled();
}
