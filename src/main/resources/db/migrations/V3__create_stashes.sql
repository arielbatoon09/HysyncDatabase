-- Migration V3: Create Stash tables
CREATE TABLE IF NOT EXISTS player_stashes (
    player_uuid TEXT NOT NULL REFERENCES players(uuid) ON DELETE CASCADE,
    stash_name TEXT NOT NULL,
    stash_size INT NOT NULL DEFAULT 54,
    items_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (player_uuid, stash_name)
);

CREATE TABLE IF NOT EXISTS player_stash_settings (
    player_uuid TEXT PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    max_stashes INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE player_stashes IS 'Player Stash storage containers';
COMMENT ON TABLE player_stash_settings IS 'Player specific Stash settings (limits)';
