package com.stephanofer.prismapractice.api.profile;

import com.stephanofer.prismapractice.api.common.PlayerId;

import java.util.Optional;

public interface ProfileRepository {

    Optional<PracticeProfile> findProfile(PlayerId playerId);

    PracticeProfile saveProfile(PracticeProfile profile);

    Optional<PracticeSettings> findSettings(PlayerId playerId);

    PracticeSettings saveSettings(PracticeSettings settings);
}
