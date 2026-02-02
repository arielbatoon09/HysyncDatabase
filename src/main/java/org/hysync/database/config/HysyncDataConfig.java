package org.hysync.database.config;

import com.google.gson.annotations.SerializedName;

/**
 * Root structure for mods/HysyncData/config.json.
 */
public class HysyncDataConfig {

    @SerializedName("database")
    private DatabaseSection database = new DatabaseSection();

    public DatabaseSection getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseSection database) {
        this.database = database;
    }

    /** Database credentials block in config.json */
    public static class DatabaseSection {
        private String host = "localhost";
        private int port = 5432;
        private String name = "hysync";
        private String username = "postgres";
        private String password = "";
        @SerializedName("ssl")
        private boolean useSSL = false;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isUseSSL() { return useSSL; }
        public void setUseSSL(boolean useSSL) { this.useSSL = useSSL; }
    }

    public DatabaseConfig toDatabaseConfig() {
        DatabaseSection db = getDatabase();
        return new DatabaseConfig(
            db.getHost(),
            db.getPort(),
            db.getName(),
            db.getUsername(),
            db.getPassword() != null ? db.getPassword() : "",
            db.isUseSSL()
        );
    }
}
