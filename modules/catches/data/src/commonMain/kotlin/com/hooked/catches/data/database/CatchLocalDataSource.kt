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

class CatchLocalDataSource(
    private val database: CatchDatabase
) {
    private val queries = database.catchQueries

    fun getAllCatches(): Flow<List<CatchEntity>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun getCatchById(id: Long): CatchEntity? {
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
                    weight = catch.weight,
                    length = catch.length,
                    photoUrl = catch.photoUrl,
                    latitude = catch.latitude,
                    longitude = catch.longitude,
                    timestamp = catch.timestamp,
                    dateCaught = "2023-10-01", // TODO: Use proper date from DTO
                    location = if (catch.latitude != null && catch.longitude != null) {
                        "${catch.latitude}, ${catch.longitude}"
                    } else "Unknown",
                    description = "Caught a ${catch.species} weighing ${catch.weight} kg"
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

    suspend fun deleteCatch(id: Long) {
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