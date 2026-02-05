package org.hysync.database.api;

import java.util.List;
import java.util.Optional;

/**
 * API for cross-server Stash sync.
 */
public interface StashSyncService {

    /**
     * Get a specific stash for a player.
     * 
     * @param playerUuid Player UUID
     * @param stashName  Name of the stash
     * @return Stash model or empty if not found
     */
    Optional<StashModel> getStash(String playerUuid, String stashName);

    /**
     * Get all stashes for a player.
     * 
     * @param playerUuid Player UUID
     * @return List of stashes
     */
    List<StashModel> getStashes(String playerUuid);

    /**
     * Save a stash (create or update).
     * 
     * @param playerUuid Player UUID
     * @param name       Stash name
     * @param size       Stash size
     * @param itemsJson  Items in JSON format
     * @return true if successful
     */
    boolean saveStash(String playerUuid, String name, int size, String itemsJson);

    /**
     * Delete a stash.
     * 
     * @param playerUuid Player UUID
     * @param stashName  Stash name
     * @return true if successful
     */
    boolean deleteStash(String playerUuid, String stashName);

    /**
     * Rename a stash.
     * 
     * @param playerUuid Player UUID
     * @param oldName    Old stash name
     * @param newName    New stash name
     * @return true if successful
     */
    boolean renameStash(String playerUuid, String oldName, String newName);

    /**
     * Get max allowed stashes for a player.
     * 
     * @param playerUuid Player UUID
     * @return max stashes count (0 means default/unlimited depending on logic)
     */
    int getMaxStashes(String playerUuid);

    /**
     * Set max allowed stashes for a player.
     * 
     * @param playerUuid Player UUID
     * @param maxStashes Max stashes count
     * @return true if successful
     */
    boolean setMaxStashes(String playerUuid, int maxStashes);

    /**
     * Unload local cache for a player (e.g. on disconnect).
     * 
     * @param playerUuid Player UUID
     */
    void unloadCache(String playerUuid);

    /**
     * Simple Stash Model record.
     */
    record StashModel(String ownerUuid, String name, int size, String itemsJson) {
    }
}
