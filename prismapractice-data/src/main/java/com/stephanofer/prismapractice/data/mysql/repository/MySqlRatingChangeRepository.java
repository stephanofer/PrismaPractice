package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.rating.RatingChange;
import com.stephanofer.prismapractice.api.rating.RatingChangeRepository;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.util.Objects;

public final class MySqlRatingChangeRepository implements RatingChangeRepository {

    private final MySqlStorage storage;

    public MySqlRatingChangeRepository(MySqlStorage storage) { this.storage = Objects.requireNonNull(storage, "storage"); }

    @Override
    public boolean exists(MatchId matchId, PlayerId playerId) {
        return storage.jdbcExecutor().queryOne("SELECT 1 FROM practice_match_rating_changes WHERE match_id = ? AND player_id = ?", st -> {
            st.setString(1, matchId.toString());
            st.setString(2, playerId.toString());
        }, rs -> 1).isPresent();
    }

    @Override
    public RatingChange save(RatingChange ratingChange) {
        storage.jdbcExecutor().update("""
                INSERT INTO practice_match_rating_changes (
                    match_id, player_id, mode_id, before_sr, after_sr, delta, before_rank_key, after_rank_key, global_before, global_after, applied_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, st -> {
            st.setString(1, ratingChange.matchId().toString());
            st.setString(2, ratingChange.playerId().toString());
            st.setString(3, ratingChange.modeId().toString());
            st.setInt(4, ratingChange.beforeSr());
            st.setInt(5, ratingChange.afterSr());
            st.setInt(6, ratingChange.delta());
            st.setString(7, ratingChange.beforeRankKey());
            st.setString(8, ratingChange.afterRankKey());
            st.setInt(9, ratingChange.globalBefore());
            st.setInt(10, ratingChange.globalAfter());
            st.setTimestamp(11, Timestamp.from(ratingChange.appliedAt()));
        });
        return ratingChange;
    }
}
