package org.hysync.database.tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hysync.database.api.InventorySyncService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reusable migration: read player JSON files from a directory and upsert into the database
 * via {@link InventorySyncService}. Used by the CLI tool and the in-game migrate command.
 */
public final class PlayerFileMigrationRunner {

    private static final Pattern UUID_STRING = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern UUID_FILE = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.json");
    private static final Gson GSON = new Gson();

    public static final class Result {
        public final int ok;
        public final int skip;
        public final int err;
        public final String message;

        public Result(int ok, int skip, int err, String message) {
            this.ok = ok;
            this.skip = skip;
            this.err = err;
            this.message = message;
        }
    }

    /**
     * Run migration: scan playersDir and upsert each player's inventory (and hotbar) into the DB.
     *
     * @param playersDir path to universe/players (or folder containing &lt;uuid&gt;/ or &lt;uuid&gt;.json)
     * @param service    InventorySyncService (from HysyncDatabase plugin)
     * @return result with ok/skip/err counts and message (e.g. error if dir missing)
     */
    public static Result run(Path playersDir, InventorySyncService service) {
        if (service == null) {
            return new Result(0, 0, 0, "Database service not available.");
        }
        if (!Files.isDirectory(playersDir)) {
            return new Result(0, 0, 0, "Not a directory: " + playersDir);
        }
        List<Path> toProcess;
        try {
            toProcess = collectPlayerFiles(playersDir);
        } catch (IOException e) {
            return new Result(0, 0, 0, "Failed to list directory: " + e.getMessage());
        }
        int ok = 0, skip = 0, err = 0;
        for (Path file : toProcess) {
            String playerUuid = resolveUuid(playersDir, file);
            if (playerUuid == null) continue;
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root == null || !root.has("Components")) {
                    skip++;
                    continue;
                }
                JsonObject components = root.getAsJsonObject("Components");
                JsonObject player = components.has("Player") ? components.getAsJsonObject("Player") : null;
                if (player == null || !player.has("Inventory")) {
                    skip++;
                    continue;
                }
                String displayName = getDisplayName(components);
                JsonObject inventory = player.getAsJsonObject("Inventory");
                int version = inventory.has("Version") ? inventory.get("Version").getAsInt() : 4;
                String inventoryJson = GSON.toJson(inventory);
                if (!service.setInventory(playerUuid, displayName, inventoryJson, version)) {
                    err++;
                    continue;
                }
                if (player.has("HotbarManager")) {
                    String hotbarJson = GSON.toJson(player.getAsJsonObject("HotbarManager"));
                    service.setHotbarManager(playerUuid, hotbarJson);
                }
                ok++;
            } catch (Exception e) {
                err++;
            }
        }
        return new Result(ok, skip, err, null);
    }

    private static List<Path> collectPlayerFiles(Path playersDir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (var stream = Files.list(playersDir)) {
            var children = stream.toList();
            for (Path child : children) {
                if (Files.isDirectory(child)) {
                    String name = child.getFileName() != null ? child.getFileName().toString() : "";
                    if (UUID_STRING.matcher(name).matches()) {
                        Path json = findFirstJsonInDir(child);
                        if (json != null) out.add(json);
                    }
                } else if (child.getFileName() != null && UUID_FILE.matcher(child.getFileName().toString()).matches()) {
                    out.add(child);
                }
            }
        }
        return out;
    }

    private static Path findFirstJsonInDir(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream
                .filter(p -> p.getFileName() != null && p.getFileName().toString().toLowerCase().endsWith(".json"))
                .findFirst()
                .orElse(null);
        }
    }

    private static String resolveUuid(Path playersDir, Path playerFile) {
        Path parent = playerFile.getParent();
        if (parent == null) return null;
        String fileName = playerFile.getFileName() != null ? playerFile.getFileName().toString() : "";
        if (parent.equals(playersDir) && fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        if (parent.getFileName() != null && UUID_STRING.matcher(parent.getFileName().toString()).matches()) {
            return parent.getFileName().toString();
        }
        return null;
    }

    private static String getDisplayName(JsonObject components) {
        if (components.has("Nameplate")) {
            var nameplate = components.getAsJsonObject("Nameplate");
            if (nameplate.has("Text") && !nameplate.get("Text").isJsonNull()) {
                return nameplate.get("Text").getAsString();
            }
        }
        if (components.has("DisplayName")) {
            var displayName = components.getAsJsonObject("DisplayName");
            if (displayName.has("DisplayName") && !displayName.get("DisplayName").isJsonNull()) {
                var inner = displayName.getAsJsonObject("DisplayName");
                if (inner.has("RawText") && !inner.get("RawText").isJsonNull()) {
                    return inner.get("RawText").getAsString();
                }
            }
        }
        return null;
    }
}
