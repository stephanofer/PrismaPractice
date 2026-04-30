package com.stephanofer.prismapractice.api.rating;

import java.util.Objects;

public record RankTier(String rankKey, String displayName, int minSr, Integer maxSr, int sortOrder, boolean enabled) {

    public RankTier {
        Objects.requireNonNull(rankKey, "rankKey");
        Objects.requireNonNull(displayName, "displayName");
    }

    public boolean matches(int sr) {
        return sr >= minSr && (maxSr == null || sr <= maxSr);
    }
}
