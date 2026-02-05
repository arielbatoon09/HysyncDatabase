package org.hysync.database.core;

import org.hysync.database.api.VoteSyncService;
import org.hysync.database.repository.VoteDAO;
import com.hypixel.hytale.logger.HytaleLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VoteSyncServiceImpl implements VoteSyncService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final DatabaseManager databaseManager;
    private final VoteDAO voteDAO;

    public VoteSyncServiceImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.voteDAO = new VoteDAO();
    }

    @Override
    public int addVote(String playerUuid, String platform, int count) {
        try (Connection conn = databaseManager.getConnection()) {
            voteDAO.addVote(conn, playerUuid, platform, count);
            return voteDAO.getTotalVotes(conn, playerUuid);
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to add vote for %s on %s", playerUuid, platform);
            return -1;
        }
    }

    @Override
    public int getTotalVotes(String playerUuid) {
        try (Connection conn = databaseManager.getConnection()) {
            return voteDAO.getTotalVotes(conn, playerUuid);
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to get total votes for %s", playerUuid);
            return 0;
        }
    }

    @Override
    public List<TopVoter> getTopVoters(int limit) {
        try (Connection conn = databaseManager.getConnection()) {
            return voteDAO.getTopVoters(conn, limit).stream()
                    .map(v -> new TopVoter(v.name, v.votes))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to get top voters");
            return Collections.emptyList();
        }
    }
}
