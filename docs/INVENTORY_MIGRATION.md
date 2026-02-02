# Cross-Server Inventory: Migration & API Plan

## Goal

- Store player inventory in PostgreSQL so it can be shared across Hytale servers.
- Expose an API so **other plugins** can read/write inventory (and optionally hotbar presets) for cross-server sync.

## Source Format (Hytale player JSON)

The inventory lives under `Components.Player.Inventory`:

| Field | Type | Description |
|-------|------|-------------|
| `Version` | int | Inventory schema version (e.g. 4) |
| `Storage` | object | Main bag: `Id`, `Capacity`, `Items` (slot index → item) |
| `Armor` | object | Same shape, capacity 4 |
| `HotBar` | object | Same shape, capacity 9 |
| `Utility` | object | Same shape, capacity 4 |
| `Backpack` | object | `Id` only (e.g. `"Empty"`) |
| `Tool` | object | Same shape, capacity 23 |
| `ActiveHotbarSlot` | int | Currently selected hotbar slot |
| `ActiveToolsSlot` | int | Currently selected tool slot |
| `ActiveUtilitySlot` | int | Currently selected utility slot |
| `SortType` | string | e.g. `"Name"` |

Each **item** in `Items`:

| Field | Type |
|-------|------|
| `Id` | string (item type, e.g. `Rock_Crystal_Green_Medium`) |
| `Quantity` | int |
| `Durability` | double |
| `MaxDurability` | double |
| `OverrideDroppedItemAnimation` | boolean |

Player identity: `Components.Player.UUID` (binary UUID) and `Components.Nameplate.Text` / `DisplayName.RawText` (display name).

---

## Database Schema (Migration)

We store the **exact** Inventory JSON (and optionally HotbarManager) so we can round-trip to/from the game without losing fields.

### Tables

1. **`players`** – Global player identity (one row per player across servers).
   - `uuid` (TEXT, PK) – Standard UUID string (e.g. from filename or `Components.Player.UUID`).
   - `display_name` (TEXT, nullable) – Last known display name.
   - `created_at`, `updated_at` (TIMESTAMPTZ).

2. **`player_inventory`** – One row per player; full Inventory stored as JSONB.
   - `player_uuid` (TEXT, PK, FK → players.uuid).
   - `inventory_version` (INT) – Same as `Inventory.Version`.
   - `inventory_json` (JSONB) – Full `Components.Player.Inventory` (Storage, Armor, HotBar, Utility, Backpack, Tool, slots, SortType).
   - `hotbar_manager_json` (JSONB, nullable) – Optional `HotbarManager` (SavedHotbars, CurrentHotbar) for cross-server hotbar presets.
   - `updated_at` (TIMESTAMPTZ).

Indexes: PKs only for now; add GIN on `inventory_json` later if we need to query by item id.

---

## API for Other Plugins

Other plugins will depend on HysyncDatabase (or call a shared API) to:

- **Get** a player’s inventory (by UUID) → JSON string or DTO.
- **Set** a player’s inventory (by UUID) → persist and optionally broadcast.
- **Get** player info (display name, last updated).

Planned entry points:

- `InventorySyncService.getInventory(playerUuid)` → full Inventory JSON.
- `InventorySyncService.setInventory(playerUuid, inventoryJson)`.
- `InventorySyncService.getHotbarManager(playerUuid)` / `setHotbarManager(...)` (optional).
- `InventorySyncService.getPlayer(playerUuid)` → display name, updated_at.

Implementation will use the existing `DatabaseManager` (HikariCP) and the new tables above.

---

## Migration Files

- `src/main/resources/db/migrations/V1__create_players_and_inventory.sql` – Creates `players` and `player_inventory` with the schema above.

**Run the migration once** against your PostgreSQL database (e.g. with `psql`, pgAdmin, or a Flyway/Liquibase step). After that, the plugin’s `InventorySyncService` uses these tables for get/set.

## API Usage (Other Plugins)

1. Get the HysyncDatabase plugin instance from Hytale’s plugin manager (mechanism depends on Hytale’s API).
2. Get `DatabasePlugin` from it, then `getInventorySyncService()`.
3. Call `getInventory(uuid)`, `setInventory(uuid, displayName, json, version)`, `getHotbarManager(uuid)`, `setHotbarManager(uuid, json)`, or `getPlayer(uuid)` as needed.