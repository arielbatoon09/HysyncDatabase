package org.hysync.database.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.hysync.database.HysyncDatabase;
import org.hysync.database.tools.PlayerFileMigrationRunner;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * In-game command: /migrateinventory [path]
 * Migrates player JSON files from universe/players (or given path) into the shared database.
 * Default path: server run dir + "universe/players".
 * Requires op or permission (if Hytale supports it).
 */
public class MigrateInventoryCommand extends AbstractAsyncCommand {

    private final HysyncDatabase plugin;
    private final OptionalArg<String> pathArg = this.withOptionalArg("path", "Path to players directory (default: universe/players)", ArgTypes.STRING);

    public MigrateInventoryCommand(HysyncDatabase plugin) {
        super("migrateinventory", "Migrate player files from disk to the inventory database");
        this.plugin = plugin;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        String pathArgValue = pathArg.get(ctx);
        Path playersDir = pathArgValue != null && !pathArgValue.isEmpty()
            ? Paths.get(pathArgValue).toAbsolutePath().normalize()
            : Paths.get(System.getProperty("user.dir", ".")).resolve("universe").resolve("players");

        ctx.sendMessage(Message.raw("[HysyncDB] Migration started from: " + playersDir).color(Color.GRAY));

        return CompletableFuture.runAsync(() -> {
            var service = plugin.getInventorySyncService();
            PlayerFileMigrationRunner.Result result = PlayerFileMigrationRunner.run(playersDir, service);

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
