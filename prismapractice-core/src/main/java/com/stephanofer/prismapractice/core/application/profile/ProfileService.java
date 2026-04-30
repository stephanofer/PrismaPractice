package com.stephanofer.prismapractice.core.application.profile;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ProfileService {

    private final ProfileRepository profileRepository;
    private final Clock clock;

    public ProfileService(ProfileRepository profileRepository, Clock clock) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ProfileBootstrapResult ensureProfile(PlayerId playerId, String currentName) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(currentName, "currentName");
        Instant now = Instant.now(clock);

        PracticeProfile profile = profileRepository.findProfile(playerId)
                .map(existing -> existing.touch(currentName, now))
                .orElseGet(() -> PracticeProfile.bootstrap(playerId, currentName, now));

        PracticeSettings settings = profileRepository.findSettings(playerId)
                .orElseGet(() -> PracticeSettings.defaults(playerId));

        return new ProfileBootstrapResult(
                profileRepository.saveProfile(profile),
                profileRepository.saveSettings(settings)
        );
    }

    public record ProfileBootstrapResult(PracticeProfile profile, PracticeSettings settings) {
    }
}
