package com.ecosys

import java.sql.Connection
import java.sql.Driver

class SQLiteConnection {
    private Connection connection = null
    private static final String DB_PATH = "/data/jenkins.db"

    public SQLiteConnection() {
        initializeConnection()
    }

    @NonCPS
    private void initializeConnection() {
        try {
            def props = new Properties()
            def driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
            connection = driver.connect("jdbc:sqlite:${DB_PATH}", props)
            if (connection == null) {
                throw new RuntimeException("Failed to establish SQLite connection: returned null")
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite connection", e)
        }
    }

    @NonCPS
    public Connection GetConnection() {
        if (connection == null) {
            throw new IllegalStateException("Connection has not been initialized")
        }
        return connection
    }
}
