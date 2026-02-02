package org.hysync.database.api;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO for cross-server player info (display name, last updated).
 */
public final class PlayerInfo implements InventorySyncService.PlayerInfo {

    private final String uuid;
    private final String displayName;
    private final Instant updatedAt;

    public PlayerInfo(String uuid, String displayName, Instant updatedAt) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.displayName = displayName;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
