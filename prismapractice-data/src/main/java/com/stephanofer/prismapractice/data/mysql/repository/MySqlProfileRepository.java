package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.matchmaking.PingRangePreference;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.profile.ProfileVisibility;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class MySqlProfileRepository implements ProfileRepository {

    private final MySqlStorage storage;

    public MySqlProfileRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Optional<PracticeProfile> findProfile(PlayerId playerId) {
        return storage.jdbcExecutor().queryOne(
                """
                SELECT player_id, current_name, normalized_name, first_seen_at, last_seen_at, profile_visibility,
                       current_global_rating, current_global_rank_key
                FROM practice_players
                WHERE player_id = ?
                """,
                statement -> statement.setString(1, playerId.toString()),
                resultSet -> new PracticeProfile(
                        PlayerId.fromString(resultSet.getString("player_id")),
                        resultSet.getString("current_name"),
                        resultSet.getString("normalized_name"),
                        resultSet.getTimestamp("first_seen_at").toInstant(),
                        resultSet.getTimestamp("last_seen_at").toInstant(),
                        ProfileVisibility.valueOf(resultSet.getString("profile_visibility")),
                        resultSet.getInt("current_global_rating"),
                        resultSet.getString("current_global_rank_key")
                )
        );
    }

    @Override
    public PracticeProfile saveProfile(PracticeProfile profile) {
        storage.jdbcExecutor().update(
                """
                INSERT INTO practice_players (
                    player_id, current_name, normalized_name, first_seen_at, last_seen_at,
                    profile_visibility, current_global_rating, current_global_rank_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    current_name = VALUES(current_name),
                    normalized_name = VALUES(normalized_name),
                    last_seen_at = VALUES(last_seen_at),
                    profile_visibility = VALUES(profile_visibility),
                    current_global_rating = VALUES(current_global_rating),
                    current_global_rank_key = VALUES(current_global_rank_key)
                """,
                statement -> {
                    statement.setString(1, profile.playerId().toString());
                    statement.setString(2, profile.currentName());
                    statement.setString(3, profile.normalizedName());
                    statement.setTimestamp(4, Timestamp.from(profile.firstSeenAt()));
                    statement.setTimestamp(5, Timestamp.from(profile.lastSeenAt()));
                    statement.setString(6, profile.visibility().name());
                    statement.setInt(7, profile.currentGlobalRating());
                    statement.setString(8, profile.currentGlobalRankKey());
                }
        );
        return profile;
    }

    @Override
    public Optional<PracticeSettings> findSettings(PlayerId playerId) {
        return storage.jdbcExecutor().queryOne(
                """
                SELECT player_id, chat_enabled, allow_duels, friends_only_duels, allow_party_invites,
                       allow_spectators, show_lobby_players, show_scoreboard, allow_event_alerts, ping_range_preference
                FROM practice_player_settings
                WHERE player_id = ?
                """,
                statement -> statement.setString(1, playerId.toString()),
                resultSet -> new PracticeSettings(
                        PlayerId.fromString(resultSet.getString("player_id")),
                        resultSet.getBoolean("chat_enabled"),
                        resultSet.getBoolean("allow_duels"),
                        resultSet.getBoolean("friends_only_duels"),
                        resultSet.getBoolean("allow_party_invites"),
                        resultSet.getBoolean("allow_spectators"),
                        resultSet.getBoolean("show_lobby_players"),
                        resultSet.getBoolean("show_scoreboard"),
                        resultSet.getBoolean("allow_event_alerts"),
                        PingRangePreference.valueOf(resultSet.getString("ping_range_preference"))
                )
        );
    }

    @Override
    public PracticeSettings saveSettings(PracticeSettings settings) {
        storage.jdbcExecutor().update(
                """
                INSERT INTO practice_player_settings (
                    player_id, chat_enabled, allow_duels, friends_only_duels, allow_party_invites,
                    allow_spectators, show_lobby_players, show_scoreboard, allow_event_alerts, ping_range_preference,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    chat_enabled = VALUES(chat_enabled),
                    allow_duels = VALUES(allow_duels),
                    friends_only_duels = VALUES(friends_only_duels),
                    allow_party_invites = VALUES(allow_party_invites),
                    allow_spectators = VALUES(allow_spectators),
                    show_lobby_players = VALUES(show_lobby_players),
                    show_scoreboard = VALUES(show_scoreboard),
                    allow_event_alerts = VALUES(allow_event_alerts),
                    ping_range_preference = VALUES(ping_range_preference),
                    updated_at = VALUES(updated_at)
                """,
                statement -> {
                    Instant now = Instant.now();
                    statement.setString(1, settings.playerId().toString());
                    statement.setBoolean(2, settings.chatEnabled());
                    statement.setBoolean(3, settings.allowDuels());
                    statement.setBoolean(4, settings.friendsOnlyDuels());
                    statement.setBoolean(5, settings.allowPartyInvites());
                    statement.setBoolean(6, settings.allowSpectators());
                    statement.setBoolean(7, settings.showLobbyPlayers());
                    statement.setBoolean(8, settings.showScoreboard());
                    statement.setBoolean(9, settings.allowEventAlerts());
                    statement.setString(10, settings.pingRangePreference().name());
                    statement.setTimestamp(11, Timestamp.from(now));
                    statement.setTimestamp(12, Timestamp.from(now));
                }
        );
        return settings;
    }
}
