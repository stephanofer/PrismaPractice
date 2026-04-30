CREATE TABLE IF NOT EXISTS practice_rank_tiers (
    rank_key VARCHAR(32) NOT NULL PRIMARY KEY,
    display_name VARCHAR(32) NOT NULL,
    min_sr INT NOT NULL,
    max_sr INT NULL,
    sort_order INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_player_mode_ratings (
    player_id CHAR(36) NOT NULL,
    mode_id VARCHAR(64) NOT NULL,
    current_sr INT NOT NULL,
    current_rank_key VARCHAR(32) NOT NULL,
    peak_sr INT NOT NULL,
    peak_rank_key VARCHAR(32) NOT NULL,
    placements_completed BOOLEAN NOT NULL,
    placements_progress INT NOT NULL,
    season_id VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    wins INT NOT NULL,
    losses INT NOT NULL,
    PRIMARY KEY (player_id, mode_id, season_id)
);

CREATE TABLE IF NOT EXISTS practice_player_global_rating (
    player_id CHAR(36) NOT NULL,
    current_global_rating INT NOT NULL,
    current_global_rank_key VARCHAR(32) NOT NULL,
    peak_global_rating INT NOT NULL,
    peak_global_rank_key VARCHAR(32) NOT NULL,
    season_id VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (player_id, season_id)
);

CREATE TABLE IF NOT EXISTS practice_seasons (
    season_id VARCHAR(64) NOT NULL PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    closed_manually_by VARCHAR(64) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_match_rating_changes (
    match_id CHAR(36) NOT NULL,
    player_id CHAR(36) NOT NULL,
    mode_id VARCHAR(64) NOT NULL,
    before_sr INT NOT NULL,
    after_sr INT NOT NULL,
    delta INT NOT NULL,
    before_rank_key VARCHAR(32) NOT NULL,
    after_rank_key VARCHAR(32) NOT NULL,
    global_before INT NOT NULL,
    global_after INT NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    PRIMARY KEY (match_id, player_id)
);

INSERT INTO practice_rank_tiers (rank_key, display_name, min_sr, max_sr, sort_order, enabled, created_at, updated_at) VALUES
('iron', 'Iron', 0, 999, 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('gold', 'Gold', 1000, 1299, 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('emerald', 'Emerald', 1300, 1599, 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('diamond', 'Diamond', 1600, 1899, 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('netherite', 'Netherite', 1900, 2199, 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('master', 'Master', 2200, 2399, 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('grandmaster', 'Grandmaster', 2400, NULL, 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
display_name = VALUES(display_name), min_sr = VALUES(min_sr), max_sr = VALUES(max_sr), sort_order = VALUES(sort_order), enabled = VALUES(enabled), updated_at = VALUES(updated_at);

INSERT INTO practice_seasons (season_id, display_name, status, started_at, ended_at, closed_manually_by, notes, created_at)
VALUES ('season-1', 'Season 1', 'ACTIVE', CURRENT_TIMESTAMP, NULL, NULL, 'Initial season', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), status = VALUES(status), ended_at = VALUES(ended_at), closed_manually_by = VALUES(closed_manually_by), notes = VALUES(notes);
