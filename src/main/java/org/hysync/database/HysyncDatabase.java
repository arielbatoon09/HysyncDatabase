package org.hysync.database;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.hysync.database.api.InventorySyncService;
import org.hysync.database.commands.MigrateInventoryCommand;
import org.hysync.database.config.DatabaseConfig;
import org.hysync.database.config.PluginConfigLoader;
import org.hysync.database.core.DatabasePlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HysyncDatabase extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HysyncDatabase instance;

    private DatabasePlugin databasePlugin;

    public HysyncDatabase(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /** For use by other plugins (e.g. HysyncCore) to get the Inventory Sync API. */
    @Nullable
    public static HysyncDatabase getInstance() {
        return instance;
    }

    /** Cross-server inventory sync API. Returns null if DB is not available. */
    @Nullable
    public InventorySyncService getInventorySyncService() {
        return databasePlugin != null ? databasePlugin.getInventorySyncService() : null;
    }

    /** Cross-server stash sync API. Returns null if DB is not available. */
    @Nullable
    public org.hysync.database.api.StashSyncService getStashSyncService() {
        return databasePlugin != null ? databasePlugin.getStashSyncService() : null;
    }

    @Override
    public void setup() {
        LOGGER.atInfo().log("Setting up HysyncDatabase plugin");

        // Load JDBC driver in plugin classloader so DriverManager/HikariCP can find it
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.atSevere().withCause(e).log("PostgreSQL driver not on classpath");
            return;
        }

        Path basePath = Paths.get(System.getProperty("user.dir", "."));
        PluginConfigLoader configLoader = new PluginConfigLoader(basePath);

        DatabaseConfig dbConfig;
        try {
            dbConfig = configLoader.load();
            LOGGER.atInfo().log("Loaded config from " + configLoader.getConfigPath());
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load mods/HysyncData/config.json; using defaults");
            return;
        }

        try {
            databasePlugin = new DatabasePlugin(dbConfig);
            LOGGER.atInfo().log("Database connection pool started successfully");
            getCommandRegistry().registerCommand(new MigrateInventoryCommand(this));
            getCommandRegistry().registerCommand(new org.hysync.database.commands.MigrateStashCommand(this));

            getEventRegistry().registerGlobal(
                    com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class, event -> {
                        String playerUuid = event.getPlayerRef().getUuid().toString();
                        if (databasePlugin != null && databasePlugin.getStashSyncService() != null) {
                            databasePlugin.getStashSyncService().unloadCache(playerUuid);
                        }
                    });
        } catch (RuntimeException e) {
            LOGGER.atSevere().withCause(e).log("Failed to connect to database. Check mods/HysyncData/config.json");
        }
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Disabling plugin " + getName());
        if (databasePlugin != null) {
            databasePlugin.teardown();
            databasePlugin = null;
        }
        instance = null;
    }
}