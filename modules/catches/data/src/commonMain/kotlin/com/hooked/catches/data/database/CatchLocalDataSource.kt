package com.hooked.catches.data.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hooked.catches.data.model.CatchDto
import com.hooked.core.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CatchLocalDataSource(
    private val database: CatchDatabase
) {
    private val queries = database.catchQueries

    fun getAllCatches(): Flow<List<CatchEntity>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun getCatchById(id: String): CatchEntity? {
        return withContext(Dispatchers.IO) {
            queries.selectById(id).executeAsOneOrNull()
        }
    }

    suspend fun insertCatch(catch: CatchDto) {
        withContext(Dispatchers.IO) {
            try {
                queries.insert(
                    id = catch.id,
                    species = catch.species,
                    location = catch.location,
                    latitude = catch.latitude,
                    longitude = catch.longitude,
                    caught_at = catch.caughtAt,
                    notes = catch.notes,
                    weather_data = catch.weatherData?.let { Json.encodeToString(it) },
                    exif_data = catch.exifData?.let { Json.encodeToString(it) },
                    image_url = catch.imageUrl,
                    image_filename = catch.imageFilename,
                    image_content_type = catch.imageContentType,
                    image_file_size = catch.imageFileSize,
                    inserted_at = catch.insertedAt,
                    updated_at = catch.updatedAt
                )
                Logger.info("CatchLocalDataSource", "Inserted catch with id: ${catch.id}")
            } catch (e: Exception) {
                Logger.error("CatchLocalDataSource", "Failed to insert catch: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun insertCatches(catches: List<CatchDto>) {
        withContext(Dispatchers.IO) {
            try {
                catches.forEach { catch ->
                    insertCatch(catch)
                }
                Logger.info("CatchLocalDataSource", "Inserted ${catches.size} catches")
            } catch (e: Exception) {
                Logger.error("CatchLocalDataSource", "Failed to insert catches: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun deleteCatch(id: String) {
        withContext(Dispatchers.IO) {
            queries.deleteById(id)
            Logger.info("CatchLocalDataSource", "Deleted catch with id: $id")
        }
    }

    suspend fun deleteAllCatches() {
        withContext(Dispatchers.IO) {
            queries.deleteAll()
            Logger.info("CatchLocalDataSource", "Deleted all catches")
        }
    }
}