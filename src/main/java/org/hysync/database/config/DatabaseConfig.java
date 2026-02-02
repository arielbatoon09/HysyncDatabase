package org.hysync.database.config;

public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;

    public DatabaseConfig(String host, int port, String database, String username, String password, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }

    public String getJdbcUrl() {
        return String.format(
            "jdbc:postgresql://%s:%d/%s?sslmode=%s",
            host,
            port,
            database,
            useSSL ? "require" : "disable"
        );
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
