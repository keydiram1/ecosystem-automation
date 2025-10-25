package com.ecosys

import groovy.sql.Sql
import java.sql.Connection
import com.ecosys.SQLiteConnection
import com.ecosys.SQLiteAsdbRecord
import com.ecosys.SQLiteAsdbDataFetcher

class SQLiteAsdbDataLoader {
    static public void LoadAsdbData () {

        String[] asdb_download_center_versions = SQLiteAsdbDataFetcher.fetchAvailableAsdbVersionsFromDownloadCenter()
        String[] asdb_dockerhub_versions = SQLiteAsdbDataFetcher.fetchAvailableAsdbVersionsFromDockerHub()
        Connection connection = new SQLiteConnection().GetConnection()

        Sql sql = new Sql(connection)

        try {
            sql.execute("""
                CREATE TABLE IF NOT EXISTS asdb_download_center_versions (
                    version TEXT PRIMARY KEY,
                    distros TEXT NOT NULL,
                    archs TEXT NOT NULL
                )
            """)
            sql.execute("""
                CREATE TABLE IF NOT EXISTS asdb_dockerhub_versions (
                        version TEXT PRIMARY KEY
                )
            """)
        } catch (Exception e) {
            throw e
        }

        try {
            sql.execute("""
                DELETE FROM asdb_download_center_versions
                """)
            sql.execute("""
                DELETE FROM asdb_dockerhub_versions
                """)

        } catch (Exception e) {
            throw e
        }

        asdb_download_center_versions.each { version ->
            SQLiteAsdbRecord record = SQLiteAsdbDataFetcher.fetchVersionDetails(version)
            try {
                sql.execute("INSERT INTO asdb_download_center_versions (version, distros, archs) VALUES (?, ?, ?)",
                        record.getVersion(), record.getDistros(), record.getArchs())
            } catch (Exception e) {
                throw e
            }
        }

        asdb_dockerhub_versions.each { version ->
            try {
                sql.execute("INSERT INTO asdb_dockerhub_versions (version) VALUES (?)", (version))
            } catch (Exception e) {
                throw e
            }
        }

        sql.close()
    }
}
