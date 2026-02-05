package org.hysync.database.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.hysync.database.HysyncDatabase;
import org.hysync.database.tools.StashFileMigrationRunner;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * In-game command: /migratestash [path]
 * Migrates stash JSON files from data/stash (or given path) into the shared
 * database.
 */
public class MigrateStashCommand extends AbstractAsyncCommand {

    private final HysyncDatabase plugin;
    private final OptionalArg<String> pathArg = this.withOptionalArg("path",
            "Path to stash data directory (default: mods/HysyncCore/data/stash)", ArgTypes.STRING);

    public MigrateStashCommand(HysyncDatabase plugin) {
        super("migratestash", "Migrate stash files from disk to the database");
        this.plugin = plugin;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        String pathArgValue = pathArg.get(ctx);

        // Default location: mods/HysyncCore/data/stash
        // Assuming current dir is server root
        Path defaultPath = Paths.get(System.getProperty("user.dir", "."))
                .resolve("mods")
                .resolve("HysyncCore")
                .resolve("data")
                .resolve("stash");

        Path stashDir = pathArgValue != null && !pathArgValue.isEmpty()
                ? Paths.get(pathArgValue).toAbsolutePath().normalize()
                : defaultPath;

        ctx.sendMessage(Message.raw("[HysyncDB] Stash Migration started from: " + stashDir).color(Color.GRAY));

        return CompletableFuture.runAsync(() -> {
            var service = plugin.getStashSyncService();
            StashFileMigrationRunner.Result result = StashFileMigrationRunner.run(stashDir, service);

            String reply;
            if (result.message != null) {
                reply = "[HysyncDB] " + result.message;
            } else {
                reply = "[HysyncDB] Done. OK=" + result.ok + " SKIP=" + result.skip + " ERR=" + result.err;
            }
            final String msg = reply;
            ctx.sendMessage(Message.raw(msg).color(result.message != null ? Color.RED : Color.GREEN));
        });
    }
}
