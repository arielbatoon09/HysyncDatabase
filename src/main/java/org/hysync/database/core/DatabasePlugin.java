package org.hysync.database.core;

import org.hysync.database.config.DatabaseConfig;
import com.hypixel.hytale.logger.HytaleLogger;

public class DatabasePlugin {
    private DatabaseManager databaseManager;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DatabasePlugin(DatabaseConfig config) {
        databaseManager = new DatabaseManager(config);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void teardown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        LOGGER.atInfo().log("[DatabasePlugin] Plugin disabled.");
    }
}