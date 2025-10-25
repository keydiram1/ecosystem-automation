package com.ecosys

import groovy.sql.Sql
import java.sql.Connection
import com.ecosys.SQLiteConnection

class SQLiteEnvManager {

    @NonCPS
    public static void AddEnvironment(Map config) {
        Connection connection = new SQLiteConnection().GetConnection();
        Sql sql = new Sql(connection);
        try {
            sql.execute("""
            CREATE TABLE IF NOT EXISTS environments (
                gke_name TEXT PRIMARY KEY
            )
        """);
            sql.execute("""
            INSERT INTO environments (gke_name)
            VALUES (?)
            ON CONFLICT(gke_name) DO UPDATE SET
                gke_name=excluded.gke_name;
            """, config.get("gke_name"));
        } catch (Exception e) {
            throw e;
        } finally {
            sql.close();
        }
    }

    @NonCPS
    public static void RemoveEnvironment(Map config) {
        Connection connection = new SQLiteConnection().GetConnection()
        Sql sql = new Sql(connection)
        try {
            sql.execute(" DELETE FROM environments WHERE gke_name = ? ", config.get("gke_name"))
        } catch (Exception e) {
            throw e
        } finally {
            sql.close()
        }
    }

    @NonCPS
    public static boolean EnvironmentExists(Map config) {
        Connection connection = new SQLiteConnection().GetConnection()
        Sql sql = new Sql(connection)
        try {
            def result = sql.firstRow(
                    "SELECT 1 FROM environments WHERE gke_name = ? LIMIT 1",
                    [config.get("gke_name")]
            )
            return result != null
        } catch (Exception e) {
            throw e
        } finally {
            sql.close()
        }
    }
}
