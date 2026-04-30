package com.stephanofer.prismapractice.api.rating;

import java.util.Optional;

public interface SeasonContextRepository {

    Optional<SeasonContext> findActive();
}
