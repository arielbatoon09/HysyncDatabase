-- Run after V1. Single-session tracking: one server per player to prevent bypass (e.g. joining smp and main_town at once).
-- Each server that uses inventory sync has a unique serverId (e.g. "smp", "main_town", "minigames").
-- When a player joins, the server claims the session; when they leave, it releases.

CREATE TABLE IF NOT EXISTS player_sessions (
    player_uuid  TEXT PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    server_id    TEXT NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE player_sessions IS 'Which server currently has this player (one session per player across sync servers)';
COMMENT ON COLUMN player_sessions.server_id IS 'Unique server identifier from HysyncCore inventorySync.serverId';
