package org.hysync.database.core;

import org.hysync.database.api.InventorySyncService;
import org.hysync.database.config.DatabaseConfig;
import com.hypixel.hytale.logger.HytaleLogger;

public class DatabasePlugin {
    private final DatabaseManager databaseManager;
    private volatile InventorySyncService inventorySyncService;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DatabasePlugin(DatabaseConfig config) {
        databaseManager = new DatabaseManager(config);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /** Cross-server inventory API for other plugins. Tables must exist (run V1 migration first). */
    public InventorySyncService getInventorySyncService() {
        if (inventorySyncService == null) {
            synchronized (this) {
                if (inventorySyncService == null) {
                    inventorySyncService = new InventorySyncServiceImpl(databaseManager);
                }
            }
        }
        return inventorySyncService;
    }

    public void teardown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        LOGGER.atInfo().log("[DatabasePlugin] Plugin disabled.");
    }
}