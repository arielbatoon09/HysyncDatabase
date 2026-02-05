package org.hysync.database.repository;

import org.hysync.database.api.StashSyncService.StashModel;
import com.hypixel.hytale.logger.HytaleLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StashDAO {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public Optional<StashModel> getStash(Connection conn, String playerUuid, String stashName) throws SQLException {
        String sql = "SELECT stash_size, items_json FROM player_stashes WHERE player_uuid = ? AND stash_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, stashName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new StashModel(
                            playerUuid,
                            stashName,
                            rs.getInt("stash_size"),
                            rs.getString("items_json")));
                }
            }
        }
        return Optional.empty();
    }

    public List<StashModel> getStashes(Connection conn, String playerUuid) throws SQLException {
        List<StashModel> list = new ArrayList<>();
        String sql = "SELECT stash_name, stash_size, items_json FROM player_stashes WHERE player_uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new StashModel(
                            playerUuid,
                            rs.getString("stash_name"),
                            rs.getInt("stash_size"),
                            rs.getString("items_json")));
                }
            }
        }
        return list;
    }

    public void saveStash(Connection conn, String playerUuid, String name, int size, String itemsJson)
            throws SQLException {
        // Ensure player exists in players table first (simplistic check, better handled
        // by upsert upstream or cascade)
        // For now, we assume migrate/login logic handles 'players' table population.
        // We use ON CONFLICT for upsert.
        String sql = "INSERT INTO player_stashes (player_uuid, stash_name, stash_size, items_json, updated_at) " +
                "VALUES (?, ?, ?, ?::jsonb, NOW()) " +
                "ON CONFLICT (player_uuid, stash_name) DO UPDATE SET " +
                "stash_size = EXCLUDED.stash_size, " +
                "items_json = EXCLUDED.items_json, " +
                "updated_at = NOW()";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, name);
            stmt.setInt(3, size);
            stmt.setString(4, itemsJson);
            stmt.executeUpdate();
        }
    }

    public boolean deleteStash(Connection conn, String playerUuid, String stashName) throws SQLException {
        String sql = "DELETE FROM player_stashes WHERE player_uuid = ? AND stash_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, stashName);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean renameStash(Connection conn, String playerUuid, String oldName, String newName) throws SQLException {
        String sql = "UPDATE player_stashes SET stash_name = ?, updated_at = NOW() WHERE player_uuid = ? AND stash_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, playerUuid);
            stmt.setString(3, oldName);
            return stmt.executeUpdate() > 0;
        }
    }

    public int getMaxStashes(Connection conn, String playerUuid) throws SQLException {
        String sql = "SELECT max_stashes FROM player_stash_settings WHERE player_uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_stashes");
                }
            }
        }
        return 0;
    }

    public void setMaxStashes(Connection conn, String playerUuid, int maxStashes) throws SQLException {
        String sql = "INSERT INTO player_stash_settings (player_uuid, max_stashes, updated_at) " +
                "VALUES (?, ?, NOW()) " +
                "ON CONFLICT (player_uuid) DO UPDATE SET " +
                "max_stashes = EXCLUDED.max_stashes, " +
                "updated_at = NOW()";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, maxStashes);
            stmt.executeUpdate();
        }
    }
}
