package org.hysync.database.api;

import java.util.List;

public interface VoteSyncService {
    /**
     * Add votes for a player on a specific platform.
     * 
     * @param playerUuid UUID of the player
     * @param platform   Platform name (e.g., "TopG")
     * @param count      Number of votes to add
     * @return New total votes across all platforms (useful for milestone check), or
     *         -1 on error.
     */
    int addVote(String playerUuid, String platform, int count);

    /**
     * Get total votes for a player across all platforms.
     */
    int getTotalVotes(String playerUuid);

    /**
     * Get top voters.
     */
    List<TopVoter> getTopVoters(int limit);

    class TopVoter {
        public final String name;
        public final int votes;

        public TopVoter(String name, int votes) {
            this.name = name;
            this.votes = votes;
        }
    }
}
