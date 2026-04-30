package com.stephanofer.prismapractice.paper.scoreboard;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerScoreboardDataCache {

    private final ProfileRepository profileRepository;
    private final Map<PlayerId, PracticeProfile> profileCache;
    private final Map<PlayerId, PracticeSettings> settingsCache;

    public PlayerScoreboardDataCache(ProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.profileCache = new ConcurrentHashMap<>();
        this.settingsCache = new ConcurrentHashMap<>();
    }

    public Optional<PracticeProfile> profile(PlayerId playerId) {
        PracticeProfile cached = profileCache.get(playerId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<PracticeProfile> loaded = profileRepository.findProfile(playerId);
        loaded.ifPresent(profile -> profileCache.put(playerId, profile));
        return loaded;
    }

    public PracticeSettings settings(PlayerId playerId) {
        PracticeSettings cached = settingsCache.get(playerId);
        if (cached != null) {
            return cached;
        }
        PracticeSettings loaded = profileRepository.findSettings(playerId).orElseGet(() -> PracticeSettings.defaults(playerId));
        settingsCache.put(playerId, loaded);
        return loaded;
    }

    public void warm(PlayerId playerId) {
        profile(playerId);
        settings(playerId);
    }

    public void update(PracticeProfile profile, PracticeSettings settings) {
        if (profile != null) {
            profileCache.put(profile.playerId(), profile);
        }
        if (settings != null) {
            settingsCache.put(settings.playerId(), settings);
        }
    }

    public void evict(PlayerId playerId) {
        profileCache.remove(playerId);
        settingsCache.remove(playerId);
    }
}
