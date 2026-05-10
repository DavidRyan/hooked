package com.hooked.mcp.tables

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.javatime.datetime

object SkunksTable : Table("user_skunks") {
    val id               = uuid("id")
    val userId           = uuid("user_id").references(UsersTable.id)
    val location         = varchar("location", 255).nullable()
    val latitude         = double("latitude").nullable()
    val longitude        = double("longitude").nullable()
    val fishedAt         = datetime("fished_at").nullable()
    val notes            = text("notes").nullable()
    val weatherData      = jsonb("weather_data", Json, JsonElement.serializer()).nullable()
    val enrichmentStatus = bool("enrichment_status").nullable()
    val insertedAt       = datetime("inserted_at")
    val updatedAt        = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
