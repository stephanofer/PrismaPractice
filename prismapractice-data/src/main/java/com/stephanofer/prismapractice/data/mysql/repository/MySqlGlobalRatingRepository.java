package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.rating.GlobalRatingRepository;
import com.stephanofer.prismapractice.api.rating.GlobalRatingSnapshot;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MySqlGlobalRatingRepository implements GlobalRatingRepository {

    private final MySqlStorage storage;

    public MySqlGlobalRatingRepository(MySqlStorage storage) { this.storage = Objects.requireNonNull(storage, "storage"); }

    @Override
    public Optional<GlobalRatingSnapshot> find(PlayerId playerId, String seasonId) {
        return storage.jdbcExecutor().queryOne("SELECT * FROM practice_player_global_rating WHERE player_id = ? AND season_id = ?", st -> {
            st.setString(1, playerId.toString());
            st.setString(2, seasonId);
        }, rs -> new GlobalRatingSnapshot(PlayerId.fromString(rs.getString("player_id")), rs.getInt("current_global_rating"), rs.getString("current_global_rank_key"), rs.getInt("peak_global_rating"), rs.getString("peak_global_rank_key"), rs.getString("season_id"), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public List<GlobalRatingSnapshot> findBySeasonId(String seasonId) {
        return storage.jdbcExecutor().query("SELECT * FROM practice_player_global_rating WHERE season_id = ?", st -> st.setString(1, seasonId), rs -> new GlobalRatingSnapshot(PlayerId.fromString(rs.getString("player_id")), rs.getInt("current_global_rating"), rs.getString("current_global_rank_key"), rs.getInt("peak_global_rating"), rs.getString("peak_global_rank_key"), rs.getString("season_id"), rs.getTimestamp("updated_at").toInstant()));
    }

    @Override
    public GlobalRatingSnapshot save(GlobalRatingSnapshot snapshot) {
        storage.jdbcExecutor().update("""
                INSERT INTO practice_player_global_rating (
                    player_id, current_global_rating, current_global_rank_key, peak_global_rating, peak_global_rank_key, season_id, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    current_global_rating = VALUES(current_global_rating),
                    current_global_rank_key = VALUES(current_global_rank_key),
                    peak_global_rating = VALUES(peak_global_rating),
                    peak_global_rank_key = VALUES(peak_global_rank_key),
                    updated_at = VALUES(updated_at)
                """, st -> {
            st.setString(1, snapshot.playerId().toString());
            st.setInt(2, snapshot.currentGlobalRating());
            st.setString(3, snapshot.currentGlobalRankKey());
            st.setInt(4, snapshot.peakGlobalRating());
            st.setString(5, snapshot.peakGlobalRankKey());
            st.setString(6, snapshot.seasonId());
            st.setTimestamp(7, Timestamp.from(snapshot.updatedAt()));
        });
        return snapshot;
    }
}
