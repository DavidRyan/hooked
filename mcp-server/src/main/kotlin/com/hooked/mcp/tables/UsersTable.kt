package com.hooked.mcp.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id        = uuid("id")
    val email     = varchar("email", 255)
    val firstName = varchar("first_name", 255).nullable()
    val lastName  = varchar("last_name", 255).nullable()
    val isActive  = bool("is_active")

    override val primaryKey = PrimaryKey(id)
}
