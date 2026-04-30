package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaType;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.match.*;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MySqlMatchRepository implements MatchRepository {

    private final MySqlStorage storage;

    public MySqlMatchRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Optional<MatchSession> findById(MatchId matchId) {
        return storage.jdbcExecutor().queryOne(
                """
                SELECT * FROM practice_matches WHERE match_id = ?
                """,
                statement -> statement.setString(1, matchId.toString()),
                rs -> new MatchSession(
                        MatchId.fromString(rs.getString("match_id")),
                        rs.getTimestamp("created_at").toInstant(),
                        toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("ended_at")),
                        MatchStatus.valueOf(rs.getString("status")),
                        PlayerType.valueOf(rs.getString("player_type")),
                        List.of(
                                new MatchParticipant(PlayerId.fromString(rs.getString("left_player_id")), MatchSide.LEFT, PlayerStatus.IN_QUEUE, true),
                                new MatchParticipant(PlayerId.fromString(rs.getString("right_player_id")), MatchSide.RIGHT, PlayerStatus.IN_QUEUE, true)
                        ),
                        new ArenaId(rs.getString("arena_id")),
                        ArenaType.valueOf(rs.getString("arena_type")),
                        new RegionId(rs.getString("region_id")),
                        RuntimeType.valueOf(rs.getString("runtime_type")),
                        rs.getString("runtime_server_id"),
                        new EffectiveMatchConfig(
                                new com.stephanofer.prismapractice.api.common.ModeId(rs.getString("mode_id")),
                                new com.stephanofer.prismapractice.api.common.QueueId(rs.getString("queue_id")),
                                QueueType.valueOf(rs.getString("queue_type")),
                                MatchmakingProfile.valueOf(rs.getString("matchmaking_profile")),
                                rs.getInt("rounds_to_win"),
                                rs.getInt("pre_fight_delay_seconds"),
                                rs.getInt("max_duration_seconds"),
                                rs.getString("kit_reference"),
                                List.of(rs.getString("rules_csv").isBlank() ? new String[0] : rs.getString("rules_csv").split(",")),
                                rs.getBoolean("spectator_allowed"),
                                SeriesFormat.valueOf(rs.getString("series_format"))
                        ),
                        new MatchScore(rs.getInt("left_rounds_won"), rs.getInt("right_rounds_won"), rs.getInt("current_round")),
                        fromNullablePlayer(rs.getString("winner_player_id")),
                        fromNullablePlayer(rs.getString("loser_player_id")),
                        fromNullableResult(rs.getString("result_type")),
                        rs.getString("cancel_reason"),
                        rs.getString("failure_reason"),
                        rs.getBoolean("recoverable")
                )
        );
    }

    @Override
    public MatchSession save(MatchSession matchSession) {
        storage.jdbcExecutor().update(
                """
                INSERT INTO practice_matches (
                    match_id, queue_id, mode_id, player_type, queue_type, matchmaking_profile,
                    left_player_id, right_player_id, arena_id, arena_type, region_id, runtime_type, runtime_server_id,
                    rounds_to_win, pre_fight_delay_seconds, max_duration_seconds, kit_reference, rules_csv, spectator_allowed, series_format,
                    left_rounds_won, right_rounds_won, current_round, status, winner_player_id, loser_player_id, result_type,
                    cancel_reason, failure_reason, recoverable, created_at, started_at, ended_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    runtime_server_id = VALUES(runtime_server_id),
                    left_rounds_won = VALUES(left_rounds_won),
                    right_rounds_won = VALUES(right_rounds_won),
                    current_round = VALUES(current_round),
                    status = VALUES(status),
                    winner_player_id = VALUES(winner_player_id),
                    loser_player_id = VALUES(loser_player_id),
                    result_type = VALUES(result_type),
                    cancel_reason = VALUES(cancel_reason),
                    failure_reason = VALUES(failure_reason),
                    recoverable = VALUES(recoverable),
                    started_at = VALUES(started_at),
                    ended_at = VALUES(ended_at)
                """,
                st -> {
                    st.setString(1, matchSession.matchId().toString());
                    st.setString(2, matchSession.effectiveConfig().queueId().toString());
                    st.setString(3, matchSession.effectiveConfig().modeId().toString());
                    st.setString(4, matchSession.playerType().name());
                    st.setString(5, matchSession.effectiveConfig().queueType().name());
                    st.setString(6, matchSession.effectiveConfig().matchmakingProfile().name());
                    st.setString(7, matchSession.participants().get(0).playerId().toString());
                    st.setString(8, matchSession.participants().get(1).playerId().toString());
                    st.setString(9, matchSession.arenaId().toString());
                    st.setString(10, matchSession.arenaType().name());
                    st.setString(11, matchSession.regionId().value());
                    st.setString(12, matchSession.runtimeType().name());
                    st.setString(13, matchSession.runtimeServerId());
                    st.setInt(14, matchSession.effectiveConfig().effectiveRoundsToWin());
                    st.setInt(15, matchSession.effectiveConfig().effectivePreFightDelaySeconds());
                    st.setInt(16, matchSession.effectiveConfig().effectiveMaxDurationSeconds());
                    st.setString(17, matchSession.effectiveConfig().effectiveKitReference());
                    st.setString(18, String.join(",", matchSession.effectiveConfig().effectiveRules()));
                    st.setBoolean(19, matchSession.effectiveConfig().spectatorAllowed());
                    st.setString(20, matchSession.effectiveConfig().seriesFormat().name());
                    st.setInt(21, matchSession.score().leftRoundsWon());
                    st.setInt(22, matchSession.score().rightRoundsWon());
                    st.setInt(23, matchSession.score().currentRound());
                    st.setString(24, matchSession.status().name());
                    nullablePlayer(st, 25, matchSession.winnerPlayerId());
                    nullablePlayer(st, 26, matchSession.loserPlayerId());
                    nullableString(st, 27, matchSession.resultType() == null ? null : matchSession.resultType().name());
                    st.setString(28, matchSession.cancelReason());
                    st.setString(29, matchSession.failureReason());
                    st.setBoolean(30, matchSession.recoverable());
                    st.setTimestamp(31, Timestamp.from(matchSession.createdAt()));
                    nullableTimestamp(st, 32, matchSession.startedAt());
                    nullableTimestamp(st, 33, matchSession.endedAt());
                }
        );
        return matchSession;
    }

    private static void nullableTimestamp(java.sql.PreparedStatement st, int index, Instant value) throws java.sql.SQLException {
        if (value == null) st.setTimestamp(index, null); else st.setTimestamp(index, Timestamp.from(value));
    }
    private static void nullableString(java.sql.PreparedStatement st, int index, String value) throws java.sql.SQLException { st.setString(index, value); }
    private static void nullablePlayer(java.sql.PreparedStatement st, int index, PlayerId playerId) throws java.sql.SQLException { st.setString(index, playerId == null ? null : playerId.toString()); }
    private static PlayerId fromNullablePlayer(String value) { return value == null || value.isBlank() ? null : PlayerId.fromString(value); }
    private static MatchResultType fromNullableResult(String value) { return value == null || value.isBlank() ? null : MatchResultType.valueOf(value); }
    private static Instant toInstant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
