CREATE TABLE IF NOT EXISTS player_votes (
    id SERIAL PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    platform VARCHAR(64) NOT NULL,
    votes INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_player_votes_uuid ON player_votes(player_uuid);
CREATE INDEX IF NOT EXISTS idx_player_votes_platform ON player_votes(platform);
