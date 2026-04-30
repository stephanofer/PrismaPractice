package com.stephanofer.prismapractice.core.application.leaderboard;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.leaderboard.*;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.rating.*;

import java.util.List;
import java.util.Objects;

public final class LeaderboardProjectionService {

    private final LeaderboardProjectionRepository leaderboardProjectionRepository;
    private final ProfileRepository profileRepository;
    private final ModeRatingRepository modeRatingRepository;
    private final GlobalRatingRepository globalRatingRepository;
    private final SeasonContextRepository seasonContextRepository;

    public LeaderboardProjectionService(
            LeaderboardProjectionRepository leaderboardProjectionRepository,
            ProfileRepository profileRepository,
            ModeRatingRepository modeRatingRepository,
            GlobalRatingRepository globalRatingRepository,
            SeasonContextRepository seasonContextRepository
    ) {
        this.leaderboardProjectionRepository = Objects.requireNonNull(leaderboardProjectionRepository, "leaderboardProjectionRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.modeRatingRepository = Objects.requireNonNull(modeRatingRepository, "modeRatingRepository");
        this.globalRatingRepository = Objects.requireNonNull(globalRatingRepository, "globalRatingRepository");
        this.seasonContextRepository = Objects.requireNonNull(seasonContextRepository, "seasonContextRepository");
    }

    public void refreshPlayerGlobalProjection(PlayerId playerId, String explicitSeasonId) {
        Objects.requireNonNull(playerId, "playerId");
        var profile = profileRepository.findProfile(playerId).orElse(null);
        if (profile == null) {
            return;
        }
        String seasonId = resolveSeasonId(explicitSeasonId);
        if (seasonId == null) {
            return;
        }
        globalRatingRepository.find(playerId, seasonId).ifPresent(snapshot -> {
            leaderboardProjectionRepository.upsert(
                    new LeaderboardScope(LeaderboardType.GLOBAL, null, null),
                    new LeaderboardEntry(playerId, profile.currentName(), snapshot.currentGlobalRating(), snapshot.currentGlobalRankKey(), 1)
            );
            leaderboardProjectionRepository.upsert(
                    new LeaderboardScope(LeaderboardType.SEASON_GLOBAL, null, seasonId),
                    new LeaderboardEntry(playerId, profile.currentName(), snapshot.currentGlobalRating(), snapshot.currentGlobalRankKey(), 1)
            );
        });
    }

    public void refreshPlayerModeProjection(PlayerId playerId, ModeId modeId, String explicitSeasonId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(modeId, "modeId");
        var profile = profileRepository.findProfile(playerId).orElse(null);
        if (profile == null) {
            return;
        }
        String seasonId = resolveSeasonId(explicitSeasonId);
        if (seasonId == null) {
            return;
        }
        modeRatingRepository.find(playerId, modeId, seasonId).ifPresent(snapshot -> {
            leaderboardProjectionRepository.upsert(
                    new LeaderboardScope(LeaderboardType.MODE, modeId, null),
                    new LeaderboardEntry(playerId, profile.currentName(), snapshot.currentSr(), snapshot.currentRankKey(), 1)
            );
            leaderboardProjectionRepository.upsert(
                    new LeaderboardScope(LeaderboardType.SEASON_MODE, modeId, seasonId),
                    new LeaderboardEntry(playerId, profile.currentName(), snapshot.currentSr(), snapshot.currentRankKey(), 1)
            );
        });
    }

    public void rebuildGlobalLeaderboard(String explicitSeasonId) {
        String seasonId = resolveSeasonId(explicitSeasonId);
        if (seasonId == null) {
            return;
        }
        LeaderboardScope globalScope = new LeaderboardScope(LeaderboardType.GLOBAL, null, null);
        LeaderboardScope seasonScope = new LeaderboardScope(LeaderboardType.SEASON_GLOBAL, null, seasonId);
        leaderboardProjectionRepository.clear(globalScope);
        leaderboardProjectionRepository.clear(seasonScope);
        for (GlobalRatingSnapshot snapshot : globalRatingRepository.findBySeasonId(seasonId)) {
            refreshPlayerGlobalProjection(snapshot.playerId(), seasonId);
        }
    }

    public void rebuildModeLeaderboard(ModeId modeId, String explicitSeasonId) {
        Objects.requireNonNull(modeId, "modeId");
        String seasonId = resolveSeasonId(explicitSeasonId);
        if (seasonId == null) {
            return;
        }
        LeaderboardScope modeScope = new LeaderboardScope(LeaderboardType.MODE, modeId, null);
        LeaderboardScope seasonModeScope = new LeaderboardScope(LeaderboardType.SEASON_MODE, modeId, seasonId);
        leaderboardProjectionRepository.clear(modeScope);
        leaderboardProjectionRepository.clear(seasonModeScope);
        for (ModeRating snapshot : modeRatingRepository.findByModeId(modeId, seasonId)) {
            refreshPlayerModeProjection(snapshot.playerId(), modeId, seasonId);
        }
    }

    public List<LeaderboardEntry> getTop(LeaderboardQuery query) {
        Objects.requireNonNull(query, "query");
        return leaderboardProjectionRepository.top(query);
    }

    private String resolveSeasonId(String explicitSeasonId) {
        if (explicitSeasonId != null && !explicitSeasonId.isBlank()) {
            return explicitSeasonId;
        }
        return seasonContextRepository.findActive().map(SeasonContext::seasonId).orElse(null);
    }
}
