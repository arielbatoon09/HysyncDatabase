package org.hysync.database.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hysync.database.api.StashSyncService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Migration runner for Stash data.
 * Reads from data/stash structure:
 * data/stash/{uuid}/stashes.json
 * data/stash/{uuid}/{stashName}_items.json
 */
public final class StashFileMigrationRunner {

    private static final Pattern UUID_STRING = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
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

    public static Result run(Path stashDataDir, StashSyncService service) {
        if (service == null) {
            return new Result(0, 0, 0, "Database service not available.");
        }
        if (!Files.isDirectory(stashDataDir)) {
            return new Result(0, 0, 0, "Not a directory: " + stashDataDir);
        }

        List<Path> userDirs;
        try {
            userDirs = collectUserDirs(stashDataDir);
        } catch (IOException e) {
            return new Result(0, 0, 0, "Failed to list directory: " + e.getMessage());
        }

        int ok = 0, skip = 0, err = 0;

        for (Path userDir : userDirs) {
            String playerUuid = userDir.getFileName().toString();
            Path stashesFile = userDir.resolve("stashes.json");

            if (!Files.exists(stashesFile)) {
                skip++;
                continue;
            }

            try {
                String json = Files.readString(stashesFile, StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(json, JsonObject.class);

                if (root == null) {
                    skip++;
                    continue;
                }

                // Migrate Max Stashes
                if (root.has("maxStashes")) {
                    service.setMaxStashes(playerUuid, root.get("maxStashes").getAsInt());
                }

                if (root.has("stashes")) {
                    JsonArray stashesArray = root.getAsJsonArray("stashes");
                    for (JsonElement element : stashesArray) {
                        JsonObject stashObj = element.getAsJsonObject();
                        String name = stashObj.get("name").getAsString();
                        int size = stashObj.get("size").getAsInt();

                        // Read items file
                        Path itemsFile = userDir.resolve(name + "_items.json");
                        String itemsJson = "[]";
                        if (Files.exists(itemsFile)) {
                            itemsJson = Files.readString(itemsFile, StandardCharsets.UTF_8);
                        }

                        // Save to DB
                        if (service.saveStash(playerUuid, name, size, itemsJson)) {
                            ok++;
                        } else {
                            err++;
                        }
                    }
                }
            } catch (Exception e) {
                err++;
            }
        }

        return new Result(ok, skip, err, null);
    }

    private static List<Path> collectUserDirs(Path stashDataDir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (var stream = Files.list(stashDataDir)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> UUID_STRING.matcher(p.getFileName().toString()).matches())
                    .forEach(out::add);
        }
        return out;
    }
}
