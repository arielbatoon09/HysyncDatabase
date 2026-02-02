-- Cross-server inventory: player identity and full inventory JSON
-- See docs/INVENTORY_MIGRATION.md for schema and API plan.

-- Player identity (one row per player across servers)
CREATE TABLE IF NOT EXISTS players (
    uuid          TEXT PRIMARY KEY,
    display_name  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Full inventory snapshot per player (round-trip compatible with Hytale Components.Player.Inventory)
CREATE TABLE IF NOT EXISTS player_inventory (
    player_uuid        TEXT PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    inventory_version  INT NOT NULL DEFAULT 4,
    inventory_json     JSONB NOT NULL,
    hotbar_manager_json JSONB,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Optional: index for querying by item id inside inventory_json (e.g. "give item" checks)
-- CREATE INDEX idx_player_inventory_items ON player_inventory USING GIN ((inventory_json -> 'Storage' -> 'Items'));
-- Add more GIN indexes per container (Armor, HotBar, Utility, Tool) if needed.

COMMENT ON TABLE players IS 'Global player identity for cross-server inventory sync';
COMMENT ON TABLE player_inventory IS 'Full Inventory + optional HotbarManager JSON per player';
COMMENT ON COLUMN player_inventory.inventory_json IS 'Components.Player.Inventory (Storage, Armor, HotBar, Utility, Backpack, Tool, slots, SortType)';
COMMENT ON COLUMN player_inventory.hotbar_manager_json IS 'Components.Player.HotbarManager (SavedHotbars, CurrentHotbar)';