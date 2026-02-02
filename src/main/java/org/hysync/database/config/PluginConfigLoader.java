package org.hysync.database.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads mods/HysyncData/config.json and creates default config if missing.
 * Config path: {basePath}/mods/HysyncData/config.json (e.g. relative to server run dir).
 */
public class PluginConfigLoader {

    private static final String CONFIG_DIR = "mods/HysyncData";
    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataFolder;

    public PluginConfigLoader(Path basePath) {
        this.dataFolder = basePath.resolve(CONFIG_DIR);
    }

    /**
     * Uses current working directory (server run dir) as base for mods/HysyncData.
     */
    public static PluginConfigLoader fromWorkingDirectory() {
        return new PluginConfigLoader(Paths.get(System.getProperty("user.dir", ".")));
    }

    public Path getConfigPath() {
        return dataFolder.resolve(CONFIG_FILE);
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    /**
     * Load config from mods/HysyncData/config.json. Creates directory and default config if missing.
     *
     * @return loaded DatabaseConfig, or null if file is missing and default could not be written
     */
    public DatabaseConfig load() throws IOException {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        String json = Files.readString(configPath, StandardCharsets.UTF_8);
        HysyncDataConfig parsed = GSON.fromJson(json, HysyncDataConfig.class);
        if (parsed == null || parsed.getDatabase() == null) {
            parsed = new HysyncDataConfig();
        }
        return parsed.toDatabaseConfig();
    }

    private void createDefaultConfig(Path configPath) throws IOException {
        Files.createDirectories(configPath.getParent());
        HysyncDataConfig defaultConfig = new HysyncDataConfig();
        String json = GSON.toJson(defaultConfig);
        Files.writeString(configPath, json, StandardCharsets.UTF_8);
    }
}
