package com.stephanofer.prismapractice.core.application.profile;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.debug.DebugCategories;
import com.stephanofer.prismapractice.debug.DebugController;
import com.stephanofer.prismapractice.debug.DebugDetailLevel;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ProfileService {

    private final ProfileRepository profileRepository;
    private final Clock clock;
    private final DebugController debug;

    public ProfileService(ProfileRepository profileRepository, Clock clock) {
        this(profileRepository, clock, DebugController.noop());
    }

    public ProfileService(ProfileRepository profileRepository, Clock clock, DebugController debug) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.debug = Objects.requireNonNull(debug, "debug");
    }

    public ProfileBootstrapResult ensureProfile(PlayerId playerId, String currentName) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(currentName, "currentName");
        Instant now = Instant.now(clock);

        java.util.Optional<PracticeProfile> existingProfile = profileRepository.findProfile(playerId);
        java.util.Optional<PracticeSettings> existingSettings = profileRepository.findSettings(playerId);

        PracticeProfile profile = existingProfile
                .map(existing -> existing.touch(currentName, now))
                .orElseGet(() -> PracticeProfile.bootstrap(playerId, currentName, now));

        PracticeSettings settings = existingSettings
                .orElseGet(() -> PracticeSettings.defaults(playerId));

        debug.info(
                DebugCategories.PROFILE,
                DebugDetailLevel.VERBOSE,
                "profile.ensure.completed",
                "Profile bootstrap resolved",
                debug.context()
                        .player(playerId.value().toString(), currentName)
                        .field("profileExisted", existingProfile.isPresent())
                        .field("settingsExisted", existingSettings.isPresent())
                        .build()
        );

        return new ProfileBootstrapResult(
                profileRepository.saveProfile(profile),
                profileRepository.saveSettings(settings)
        );
    }

    public record ProfileBootstrapResult(PracticeProfile profile, PracticeSettings settings) {
    }
}
