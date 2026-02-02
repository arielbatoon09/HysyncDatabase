package org.hysync.database.core;

import org.hysync.database.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final com.zaxxer.hikari.HikariDataSource dataSource;

    public DatabaseManager(DatabaseConfig config) {
        com.zaxxer.hikari.HikariConfig hikariConfig = new com.zaxxer.hikari.HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setPoolName("HysyncHikariPool");

        com.zaxxer.hikari.HikariDataSource ds = null;
        try {
            ds = new com.zaxxer.hikari.HikariDataSource(hikariConfig);
            try (Connection conn = ds.getConnection()) {
                // Test connection
            }
        } catch (SQLException e) {
            if (ds != null) {
                ds.close();
                ds = null;
            }
            throw new RuntimeException("Failed to connect to PostgreSQL: " + e.getMessage(), e);
        }

        this.dataSource = ds;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized.");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}