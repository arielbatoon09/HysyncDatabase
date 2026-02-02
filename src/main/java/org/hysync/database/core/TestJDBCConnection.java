package org.hysync.database.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestJDBCConnection {

    public static void testConnection() {
        String url = "jdbc:postgresql://ep-patient-forest-a16mm48o-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_2XtIwoBP8lQY";

        try {
            // Load driver in plugin classloader so DriverManager can find it (Hytale loads plugins in isolated classloader)
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[HysyncDB] PostgreSQL driver not on classpath: " + e.getMessage());
            return;
        }
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("[HysyncDB] ✅ JDBC Connection successful!");
        } catch (SQLException e) {
            System.err.println("[HysyncDB] ❌ JDBC Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Optional main for standalone testing
    public static void main(String[] args) {
        testConnection();
    }
}