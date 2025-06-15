package data.repo

import data.api.HookedApiService
import data.model.CatchDto
import data.db.Database
import domain.repository.CatchRepositoy

class CatchRepository(
    private val database: Database,
    private val hookedApiService: HookedApiService
) : CatchRepositoy {

    //private val queries = database.catchQueries

    suspend fun getCatches(): List<CatchDto> {
        return hookedApiService.getCatches()
/*
        val cachedCatches = queries.selectAll().executeAsList().map {
            CatchDto(it.id, it.species, it.weight, it.length, it.photoUrl)
        }
        if (cachedCatches.isNotEmpty()) {
            return cachedCatches
        }
*/

    }
}
