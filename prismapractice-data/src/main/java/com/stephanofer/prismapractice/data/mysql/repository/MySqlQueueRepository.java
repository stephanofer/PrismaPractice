package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.queue.SearchExpansionStrategy;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MySqlQueueRepository implements QueueRepository {

    private final MySqlStorage storage;

    public MySqlQueueRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Optional<QueueDefinition> findById(QueueId queueId) {
        return storage.jdbcExecutor().queryOne(
                """
                SELECT queue_id, mode_id, display_name, queue_type, player_type, matchmaking_profile,
                       enabled, visible_in_menu, rated, uses_skill_rating, uses_ping_range,
                       uses_region_selection, search_expansion_strategy, blocked_if_in_party,
                       allowed_states_csv, blocked_states_csv,
                       affects_global_rating, affects_visible_rank, affects_season_stats,
                       affects_leaderboards, target_runtime
                FROM practice_queues
                WHERE queue_id = ?
                """,
                statement -> statement.setString(1, queueId.toString()),
                resultSet -> new QueueDefinition(
                        new QueueId(resultSet.getString("queue_id")),
                        new ModeId(resultSet.getString("mode_id")),
                        resultSet.getString("display_name"),
                        QueueType.valueOf(resultSet.getString("queue_type")),
                        PlayerType.valueOf(resultSet.getString("player_type")),
                        MatchmakingProfile.valueOf(resultSet.getString("matchmaking_profile")),
                        resultSet.getBoolean("enabled"),
                        resultSet.getBoolean("visible_in_menu"),
                        resultSet.getBoolean("rated"),
                        resultSet.getBoolean("uses_skill_rating"),
                        resultSet.getBoolean("uses_ping_range"),
                        resultSet.getBoolean("uses_region_selection"),
                        SearchExpansionStrategy.valueOf(resultSet.getString("search_expansion_strategy")),
                        resultSet.getBoolean("blocked_if_in_party"),
                        parseStatuses(resultSet.getString("allowed_states_csv")),
                        parseStatuses(resultSet.getString("blocked_states_csv")),
                        resultSet.getBoolean("affects_global_rating"),
                        resultSet.getBoolean("affects_visible_rank"),
                        resultSet.getBoolean("affects_season_stats"),
                        resultSet.getBoolean("affects_leaderboards"),
                        RuntimeType.valueOf(resultSet.getString("target_runtime"))
                )
        );
    }

    @Override
    public QueueDefinition save(QueueDefinition queueDefinition) {
        storage.jdbcExecutor().update(
                """
                INSERT INTO practice_queues (
                    queue_id, mode_id, display_name, queue_type, player_type, matchmaking_profile,
                    enabled, visible_in_menu, rated, uses_skill_rating, uses_ping_range,
                    uses_region_selection, search_expansion_strategy, blocked_if_in_party,
                    allowed_states_csv, blocked_states_csv,
                    affects_global_rating, affects_visible_rank, affects_season_stats,
                    affects_leaderboards, target_runtime, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    mode_id = VALUES(mode_id),
                    display_name = VALUES(display_name),
                    queue_type = VALUES(queue_type),
                    player_type = VALUES(player_type),
                    matchmaking_profile = VALUES(matchmaking_profile),
                    enabled = VALUES(enabled),
                    visible_in_menu = VALUES(visible_in_menu),
                    rated = VALUES(rated),
                    uses_skill_rating = VALUES(uses_skill_rating),
                    uses_ping_range = VALUES(uses_ping_range),
                    uses_region_selection = VALUES(uses_region_selection),
                    search_expansion_strategy = VALUES(search_expansion_strategy),
                    blocked_if_in_party = VALUES(blocked_if_in_party),
                    allowed_states_csv = VALUES(allowed_states_csv),
                    blocked_states_csv = VALUES(blocked_states_csv),
                    affects_global_rating = VALUES(affects_global_rating),
                    affects_visible_rank = VALUES(affects_visible_rank),
                    affects_season_stats = VALUES(affects_season_stats),
                    affects_leaderboards = VALUES(affects_leaderboards),
                    target_runtime = VALUES(target_runtime),
                    updated_at = VALUES(updated_at)
                """,
                statement -> {
                    Instant now = Instant.now();
                    statement.setString(1, queueDefinition.queueId().toString());
                    statement.setString(2, queueDefinition.modeId().toString());
                    statement.setString(3, queueDefinition.displayName());
                    statement.setString(4, queueDefinition.queueType().name());
                    statement.setString(5, queueDefinition.playerType().name());
                    statement.setString(6, queueDefinition.matchmakingProfile().name());
                    statement.setBoolean(7, queueDefinition.enabled());
                    statement.setBoolean(8, queueDefinition.visibleInMenu());
                    statement.setBoolean(9, queueDefinition.rated());
                    statement.setBoolean(10, queueDefinition.usesSkillRating());
                    statement.setBoolean(11, queueDefinition.usesPingRange());
                    statement.setBoolean(12, queueDefinition.usesRegionSelection());
                    statement.setString(13, queueDefinition.searchExpansionStrategy().name());
                    statement.setBoolean(14, queueDefinition.blockedIfInParty());
                    statement.setString(15, formatStatuses(queueDefinition.allowedStatuses()));
                    statement.setString(16, formatStatuses(queueDefinition.blockedStatuses()));
                    statement.setBoolean(17, queueDefinition.affectsGlobalRating());
                    statement.setBoolean(18, queueDefinition.affectsVisibleRank());
                    statement.setBoolean(19, queueDefinition.affectsSeasonStats());
                    statement.setBoolean(20, queueDefinition.affectsLeaderboards());
                    statement.setString(21, queueDefinition.targetRuntime().name());
                    statement.setTimestamp(22, Timestamp.from(now));
                    statement.setTimestamp(23, Timestamp.from(now));
                }
        );
        return queueDefinition;
    }

    private static Set<PlayerStatus> parseStatuses(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(PlayerStatus::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PlayerStatus.class)));
    }

    private static String formatStatuses(Set<PlayerStatus> statuses) {
        return statuses.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
