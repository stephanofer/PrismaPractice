CREATE TABLE IF NOT EXISTS practice_arenas (
    arena_id VARCHAR(64) NOT NULL PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    arena_type VARCHAR(16) NOT NULL,
    region_id VARCHAR(32) NOT NULL,
    runtime_type VARCHAR(16) NOT NULL,
    server_pool_key VARCHAR(64) NOT NULL,
    allowed_modes_csv VARCHAR(255) NOT NULL,
    blocked_modes_csv VARCHAR(255) NOT NULL,
    allowed_player_types_csv VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    selectable BOOLEAN NOT NULL,
    selection_weight INT NOT NULL,
    featured BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    KEY idx_practice_arenas_region_runtime (region_id, runtime_type),
    KEY idx_practice_arenas_enabled_selectable (enabled, selectable)
);

INSERT INTO practice_arenas (
    arena_id, display_name, arena_type, region_id, runtime_type, server_pool_key,
    allowed_modes_csv, blocked_modes_csv, allowed_player_types_csv,
    enabled, selectable, selection_weight, featured, created_at, updated_at
) VALUES
    ('duel-sa-01', 'Duel SA 01', 'DUEL', 'sa', 'MATCH', 'match-sa', 'sword,axe,builduhc,crystalpvp,pot,smp', '', 'ONE_VS_ONE', TRUE, TRUE, 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('duel-sa-02', 'Duel SA 02', 'DUEL', 'sa', 'MATCH', 'match-sa', 'sword,axe,builduhc,crystalpvp,pot,smp', '', 'ONE_VS_ONE', TRUE, TRUE, 90, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('duel-na-01', 'Duel NA 01', 'DUEL', 'na', 'MATCH', 'match-na', 'sword,axe,builduhc,crystalpvp,pot,smp', '', 'ONE_VS_ONE', TRUE, TRUE, 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('duel-na-02', 'Duel NA 02', 'DUEL', 'na', 'MATCH', 'match-na', 'sword,axe,builduhc,crystalpvp,pot,smp', '', 'ONE_VS_ONE', TRUE, TRUE, 90, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    arena_type = VALUES(arena_type),
    region_id = VALUES(region_id),
    runtime_type = VALUES(runtime_type),
    server_pool_key = VALUES(server_pool_key),
    allowed_modes_csv = VALUES(allowed_modes_csv),
    blocked_modes_csv = VALUES(blocked_modes_csv),
    allowed_player_types_csv = VALUES(allowed_player_types_csv),
    enabled = VALUES(enabled),
    selectable = VALUES(selectable),
    selection_weight = VALUES(selection_weight),
    featured = VALUES(featured),
    updated_at = VALUES(updated_at);
