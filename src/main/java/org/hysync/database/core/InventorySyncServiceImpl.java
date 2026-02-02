package org.hysync.database.core;

import org.hysync.database.api.InventorySyncService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of cross-server inventory sync using the players and player_inventory tables.
 * Assumes migration V1__create_players_and_inventory.sql has been applied.
 */
public class InventorySyncServiceImpl implements InventorySyncService {

    private static final String GET_INVENTORY =
        "SELECT inventory_json FROM player_inventory WHERE player_uuid = ?";
    private static final String SET_INVENTORY_PLAYER =
        "INSERT INTO players (uuid, display_name, updated_at) VALUES (?, ?, NOW()) " +
        "ON CONFLICT (uuid) DO UPDATE SET display_name = COALESCE(EXCLUDED.display_name, players.display_name), updated_at = NOW()";
    private static final String SET_INVENTORY =
        "INSERT INTO player_inventory (player_uuid, inventory_version, inventory_json, updated_at) VALUES (?, ?, ?::jsonb, NOW()) " +
        "ON CONFLICT (player_uuid) DO UPDATE SET inventory_version = EXCLUDED.inventory_version, inventory_json = EXCLUDED.inventory_json, updated_at = NOW()";
    private static final String GET_HOTBAR_MANAGER =
        "SELECT hotbar_manager_json FROM player_inventory WHERE player_uuid = ?";
    private static final String SET_HOTBAR_MANAGER =
        "UPDATE player_inventory SET hotbar_manager_json = ?::jsonb, updated_at = NOW() WHERE player_uuid = ?";
    private static final String GET_PLAYER =
        "SELECT uuid, display_name, updated_at FROM players WHERE uuid = ?";

    private final DatabaseManager databaseManager;

    public InventorySyncServiceImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Optional<String> getInventory(String playerUuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_INVENTORY)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString(1);
                    return Optional.ofNullable(json);
                }
            }
        } catch (SQLException e) {
            // Log and return empty
        }
        return Optional.empty();
    }

    @Override
    public boolean setInventory(String playerUuid, @Nullable String displayName, String inventoryJson, int inventoryVersion) {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SET_INVENTORY_PLAYER)) {
                    ps.setString(1, playerUuid);
                    ps.setString(2, displayName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SET_INVENTORY)) {
                    ps.setString(1, playerUuid);
                    ps.setInt(2, inventoryVersion);
                    ps.setString(3, inventoryJson);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Optional<String> getHotbarManager(String playerUuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_HOTBAR_MANAGER)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString(1);
                    return Optional.ofNullable(json);
                }
            }
        } catch (SQLException e) {
            // Log and return empty
        }
        return Optional.empty();
    }

    @Override
    public boolean setHotbarManager(String playerUuid, String hotbarManagerJson) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SET_HOTBAR_MANAGER)) {
            ps.setString(1, hotbarManagerJson);
            ps.setString(2, playerUuid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Optional<PlayerInfo> getPlayer(String playerUuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_PLAYER)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uuid = rs.getString(1);
                    String displayName = rs.getString(2);
                    Timestamp ts = rs.getTimestamp(3);
                    Instant updatedAt = ts != null ? ts.toInstant() : Instant.EPOCH;
                    return Optional.of(new org.hysync.database.api.PlayerInfo(uuid, displayName, updatedAt));
                }
            }
        } catch (SQLException e) {
            // Log and return empty
        }
        return Optional.empty();
    }
}
