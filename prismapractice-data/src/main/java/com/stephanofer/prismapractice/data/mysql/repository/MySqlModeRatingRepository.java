package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.rating.ModeRating;
import com.stephanofer.prismapractice.api.rating.ModeRatingRepository;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MySqlModeRatingRepository implements ModeRatingRepository {

    private final MySqlStorage storage;

    public MySqlModeRatingRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Optional<ModeRating> find(PlayerId playerId, ModeId modeId, String seasonId) {
        return storage.jdbcExecutor().queryOne("""
                SELECT * FROM practice_player_mode_ratings WHERE player_id = ? AND mode_id = ? AND season_id = ?
                """, st -> {
            st.setString(1, playerId.toString());
            st.setString(2, modeId.toString());
            st.setString(3, seasonId);
        }, rs -> map(rs.getString("player_id"), rs.getString("mode_id"), rs.getInt("current_sr"), rs.getString("current_rank_key"), rs.getInt("peak_sr"), rs.getString("peak_rank_key"), rs.getBoolean("placements_completed"), rs.getInt("placements_progress"), rs.getString("season_id"), rs.getTimestamp("updated_at").toInstant(), rs.getInt("wins"), rs.getInt("losses")));
    }

    @Override
    public List<ModeRating> findByPlayerId(PlayerId playerId, String seasonId) {
        return storage.jdbcExecutor().query("""
                SELECT * FROM practice_player_mode_ratings WHERE player_id = ? AND season_id = ?
                """, st -> {
            st.setString(1, playerId.toString());
            st.setString(2, seasonId);
        }, rs -> map(rs.getString("player_id"), rs.getString("mode_id"), rs.getInt("current_sr"), rs.getString("current_rank_key"), rs.getInt("peak_sr"), rs.getString("peak_rank_key"), rs.getBoolean("placements_completed"), rs.getInt("placements_progress"), rs.getString("season_id"), rs.getTimestamp("updated_at").toInstant(), rs.getInt("wins"), rs.getInt("losses")));
    }

    @Override
    public List<ModeRating> findByModeId(ModeId modeId, String seasonId) {
        return storage.jdbcExecutor().query("""
                SELECT * FROM practice_player_mode_ratings WHERE mode_id = ? AND season_id = ?
                """, st -> {
            st.setString(1, modeId.toString());
            st.setString(2, seasonId);
        }, rs -> map(rs.getString("player_id"), rs.getString("mode_id"), rs.getInt("current_sr"), rs.getString("current_rank_key"), rs.getInt("peak_sr"), rs.getString("peak_rank_key"), rs.getBoolean("placements_completed"), rs.getInt("placements_progress"), rs.getString("season_id"), rs.getTimestamp("updated_at").toInstant(), rs.getInt("wins"), rs.getInt("losses")));
    }

    @Override
    public ModeRating save(ModeRating modeRating) {
        storage.jdbcExecutor().update("""
                INSERT INTO practice_player_mode_ratings (
                    player_id, mode_id, current_sr, current_rank_key, peak_sr, peak_rank_key,
                    placements_completed, placements_progress, season_id, updated_at, wins, losses
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    current_sr = VALUES(current_sr),
                    current_rank_key = VALUES(current_rank_key),
                    peak_sr = VALUES(peak_sr),
                    peak_rank_key = VALUES(peak_rank_key),
                    placements_completed = VALUES(placements_completed),
                    placements_progress = VALUES(placements_progress),
                    updated_at = VALUES(updated_at),
                    wins = VALUES(wins),
                    losses = VALUES(losses)
                """, st -> {
            st.setString(1, modeRating.playerId().toString());
            st.setString(2, modeRating.modeId().toString());
            st.setInt(3, modeRating.currentSr());
            st.setString(4, modeRating.currentRankKey());
            st.setInt(5, modeRating.peakSr());
            st.setString(6, modeRating.peakRankKey());
            st.setBoolean(7, modeRating.placementsCompleted());
            st.setInt(8, modeRating.placementsPlayed());
            st.setString(9, modeRating.seasonId());
            st.setTimestamp(10, Timestamp.from(modeRating.updatedAt()));
            st.setInt(11, modeRating.wins());
            st.setInt(12, modeRating.losses());
        });
        return modeRating;
    }

    private ModeRating map(String playerId, String modeId, int currentSr, String currentRankKey, int peakSr, String peakRankKey, boolean placementsCompleted, int placementsPlayed, String seasonId, Instant updatedAt, int wins, int losses) {
        return new ModeRating(PlayerId.fromString(playerId), new ModeId(modeId), currentSr, currentRankKey, peakSr, peakRankKey, placementsCompleted, placementsPlayed, seasonId, updatedAt, wins, losses);
    }
}
