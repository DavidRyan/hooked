package com.hooked.mcp

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun connect(): Database {
        val rawUrl = System.getenv("DATABASE_URL")
            ?: error("DATABASE_URL environment variable is required")

        val jdbcUrl = if (rawUrl.startsWith("postgres://")) {
            rawUrl.replace("postgres://", "jdbc:postgresql://")
        } else {
            rawUrl
        }

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
            minimumIdle = 1
            connectionTimeout = 10_000
            idleTimeout = 60_000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }

        return Database.connect(HikariDataSource(config))
    }
}
