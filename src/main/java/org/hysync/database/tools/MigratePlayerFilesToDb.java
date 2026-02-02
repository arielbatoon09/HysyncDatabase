package org.hysync.database.tools;

import org.hysync.database.api.InventorySyncService;
import org.hysync.database.config.DatabaseConfig;
import org.hysync.database.config.PluginConfigLoader;
import org.hysync.database.core.DatabaseManager;
import org.hysync.database.core.InventorySyncServiceImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI entry point: read Hytale player JSON from a directory and upsert inventory + hotbar
 * into the shared PostgreSQL database. Uses {@link PlayerFileMigrationRunner}.
 * <p>
 * Example: {@code MigratePlayerFilesToDb D:/HytaleProject/Server/universe/players}
 */
public final class MigratePlayerFilesToDb {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: MigratePlayerFilesToDb <path_to_players_dir>");
            System.err.println("  Example: MigratePlayerFilesToDb D:/HytaleProject/Server/universe/players");
            System.err.println("  Run from server run dir so mods/HysyncData/config.json is used.");
            System.exit(1);
        }
        Path playersDir = Paths.get(args[0]).toAbsolutePath().normalize();

        Path configBase = Paths.get(System.getProperty("user.dir", "."));
        PluginConfigLoader configLoader = new PluginConfigLoader(configBase);
        DatabaseConfig dbConfig = configLoader.load();
        if (dbConfig == null) {
            System.err.println("Failed to load config from " + configLoader.getConfigPath());
            System.exit(1);
        }

        System.out.println("Connecting to database...");
        DatabaseManager dbManager = new DatabaseManager(dbConfig);
        InventorySyncService service = new InventorySyncServiceImpl(dbManager);
        try {
            PlayerFileMigrationRunner.Result result = PlayerFileMigrationRunner.run(playersDir, service);
            if (result.message != null) {
                System.err.println(result.message);
                System.exit(1);
            }
            System.out.println("Done. OK=" + result.ok + " SKIP=" + result.skip + " ERR=" + result.err);
        } finally {
            dbManager.shutdown();
        }
    }
}
