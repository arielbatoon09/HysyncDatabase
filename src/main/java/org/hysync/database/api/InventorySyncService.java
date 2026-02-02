package org.hysync.database.api;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * API for cross-server inventory sync. Other plugins can use this to get/set
 * player inventory (and optional hotbar presets) stored in the shared database.
 * <p>
 * Player identity is by UUID (text, e.g. standard UUID string from Hytale).
 */
public interface InventorySyncService {

    /**
     * Get full Inventory JSON for a player (Components.Player.Inventory).
     *
     * @param playerUuid player UUID (text)
     * @return Inventory JSON string, or empty if not found
     */
    Optional<String> getInventory(String playerUuid);

    /**
     * Save full Inventory JSON for a player. Creates/updates players row if needed.
     *
     * @param playerUuid   player UUID (text)
     * @param displayName   optional display name to upsert in players
     * @param inventoryJson full Inventory JSON (Storage, Armor, HotBar, Utility, Backpack, Tool, etc.)
     * @param inventoryVersion version number (e.g. 4)
     * @return true if saved, false on error
     */
    boolean setInventory(String playerUuid, @Nullable String displayName, String inventoryJson, int inventoryVersion);

    /**
     * Get HotbarManager JSON (SavedHotbars, CurrentHotbar) if stored.
     *
     * @param playerUuid player UUID (text)
     * @return HotbarManager JSON string, or empty if not stored
     */
    Optional<String> getHotbarManager(String playerUuid);

    /**
     * Save HotbarManager JSON for a player.
     *
     * @param playerUuid        player UUID (text)
     * @param hotbarManagerJson HotbarManager JSON
     * @return true if saved, false on error
     */
    boolean setHotbarManager(String playerUuid, String hotbarManagerJson);

    /**
     * Get player display name and last updated time.
     *
     * @param playerUuid player UUID (text)
     * @return optional player info, or empty if not found
     */
    Optional<PlayerInfo> getPlayer(String playerUuid);

    /**
     * Minimal player info for API consumers.
     */
    interface PlayerInfo {
        String getUuid();
        String getDisplayName();
        java.time.Instant getUpdatedAt();
    }
}