package com.ecosys

import groovy.sql.Sql
import java.sql.Connection
import com.ecosys.SQLiteConnection

class SQLiteWorkspaceManager {

    @NonCPS
    public static void AddWorkspace(Map config) {
        Connection connection = new SQLiteConnection().GetConnection();
        Sql sql = new Sql(connection);
        try {
            sql.execute("""
            CREATE TABLE IF NOT EXISTS workspaces (
                workspace TEXT PRIMARY KEY,
                asdb_version TEXT,
                asdb_size TEXT,
                backup_service_version TEXT,
                storage_provider TEXT,
                infra_branch TEXT
            )
        """);

            sql.execute("""
                        INSERT INTO workspaces (workspace, asdb_version, asdb_size, backup_service_version, storage_provider, infra_branch)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(workspace) DO UPDATE SET
                            asdb_version=excluded.asdb_version,
                            asdb_size=excluded.asdb_size,
                            backup_service_version=excluded.backup_service_version,
                            storage_provider=excluded.storage_provider,
                            infra_branch=excluded.infra_branch
                    """,
                    config.get("ws"),
                    config.get("asdb-version"),
                    config.get("asdb-size"),
                    config.get("aerospike-backup-service-version"),
                    config.get("storage-provider"),
                    config.get("infra-branch"));
        } catch (Exception e) {
            throw e;
        } finally {
            sql.close();
        }
    }


    @NonCPS
    public static void RemoveWorkspace(String workspace) {
        Connection connection = new SQLiteConnection().GetConnection()
        Sql sql = new Sql(connection)
        try {
            sql.execute(" DELETE FROM workspaces WHERE workspace = ? ", workspace)
        } catch (Exception e) {
            throw e
        } finally {
            sql.close()
        }
    }

    @NonCPS
    public static int CountWorkspaces() {
        Connection connection = new SQLiteConnection().GetConnection()
        Sql sql = new Sql(connection)
        try {
            def row = sql.firstRow("SELECT COUNT(*) AS total FROM workspaces")
            return row.total as int
        } catch (Exception e) {
            throw e
        } finally {
            sql.close()
        }
    }
}
