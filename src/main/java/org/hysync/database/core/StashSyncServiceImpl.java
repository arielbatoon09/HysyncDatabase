package org.hysync.database.core;

import org.hysync.database.api.StashSyncService;
import org.hysync.database.repository.StashDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class StashSyncServiceImpl implements StashSyncService {

    private final DatabaseManager databaseManager;
    private final StashDAO stashDAO;
    private final java.util.Map<String, List<StashModel>> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public StashSyncServiceImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.stashDAO = new StashDAO();
    }

    @Override
    public Optional<StashModel> getStash(String playerUuid, String stashName) {
        // Check cache first
        List<StashModel> cached = cache.get(playerUuid);
        if (cached != null) {
            for (StashModel s : cached) {
                if (s.name().equals(stashName))
                    return Optional.of(s);
            }
        }

        try (Connection conn = databaseManager.getConnection()) {
            Optional<StashModel> result = stashDAO.getStash(conn, playerUuid, stashName);
            // Update cache if found (and cache exists for player) or just rely on DB
            // Typically we only populate cache on full load or specific request if we want
            // partial cache
            // For "load on join" behavior, we might assume cache is populated if present
            return result;
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<StashModel> getStashes(String playerUuid) {
        if (cache.containsKey(playerUuid)) {
            return new java.util.ArrayList<>(cache.get(playerUuid));
        }

        try (Connection conn = databaseManager.getConnection()) {
            List<StashModel> list = stashDAO.getStashes(conn, playerUuid);
            cache.put(playerUuid, new java.util.concurrent.CopyOnWriteArrayList<>(list));
            return list;
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean saveStash(String playerUuid, String name, int size, String itemsJson) {
        try (Connection conn = databaseManager.getConnection()) {
            stashDAO.saveStash(conn, playerUuid, name, size, itemsJson);

            // Update cache
            List<StashModel> list = cache.computeIfAbsent(playerUuid,
                    k -> new java.util.concurrent.CopyOnWriteArrayList<>());
            list.removeIf(s -> s.name().equals(name));
            list.add(new StashModel(playerUuid, name, size, itemsJson));

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean deleteStash(String playerUuid, String stashName) {
        try (Connection conn = databaseManager.getConnection()) {
            if (stashDAO.deleteStash(conn, playerUuid, stashName)) {
                if (cache.containsKey(playerUuid)) {
                    cache.get(playerUuid).removeIf(s -> s.name().equals(stashName));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean renameStash(String playerUuid, String oldName, String newName) {
        try (Connection conn = databaseManager.getConnection()) {
            if (stashDAO.renameStash(conn, playerUuid, oldName, newName)) {
                if (cache.containsKey(playerUuid)) {
                    List<StashModel> list = cache.get(playerUuid);
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).name().equals(oldName)) {
                            StashModel old = list.get(i);
                            list.set(i, new StashModel(playerUuid, newName, old.size(), old.itemsJson()));
                            break;
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void unloadCache(String playerUuid) {
        cache.remove(playerUuid);
    }

    @Override
    public int getMaxStashes(String playerUuid) {
        try (Connection conn = databaseManager.getConnection()) {
            return stashDAO.getMaxStashes(conn, playerUuid);
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public boolean setMaxStashes(String playerUuid, int maxStashes) {
        try (Connection conn = databaseManager.getConnection()) {
            stashDAO.setMaxStashes(conn, playerUuid, maxStashes);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
