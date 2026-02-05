package org.hysync.database.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoteDAO {

    public void addVote(Connection conn, String playerUuid, String platform, int count) throws SQLException {
        String sql = "INSERT INTO player_votes (player_uuid, platform, votes, created_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            stmt.setInt(3, count);
            stmt.executeUpdate();
        }
    }

    public int getVotes(Connection conn, String playerUuid, String platform) throws SQLException {
        String sql = "SELECT SUM(votes) as total FROM player_votes WHERE player_uuid = ? AND platform = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, platform);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public int getTotalVotes(Connection conn, String playerUuid) throws SQLException {
        String sql = "SELECT SUM(votes) as total FROM player_votes WHERE player_uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public List<TopVoter> getTopVoters(Connection conn, int limit) throws SQLException {
        List<TopVoter> list = new ArrayList<>();
        // Note: Joining with players table to get display names.
        // This assumes players table is populated. If not, name might be null or we
        // fallback to UUID.
        String sql = "SELECT p.display_name, v.player_uuid, SUM(v.votes) as total " +
                "FROM player_votes v " +
                "LEFT JOIN players p ON v.player_uuid = p.uuid " +
                "GROUP BY v.player_uuid, p.display_name " +
                "ORDER BY total DESC LIMIT ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("display_name");
                    if (name == null)
                        name = rs.getString("player_uuid");
                    list.add(new TopVoter(name, rs.getInt("total")));
                }
            }
        }
        return list;
    }

    public static class TopVoter {
        public final String name;
        public final int votes;

        public TopVoter(String name, int votes) {
            this.name = name;
            this.votes = votes;
        }
    }
}
