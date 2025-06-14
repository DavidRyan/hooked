package com.hooked.data

import com.hooked.database.HookedDatabase
import com.hooked.domain.CatchModel

class CatchGridRepository(
    private val httpClient: HttpClient,
    private val database: HookedDatabase
) {

    private val queries = database.catchQueries

    suspend fun getCatches(): List<CatchModel> {
        val cachedCatches = queries.selectAll().executeAsList().map {
            CatchModel(it.id, it.species, it.weight, it.length, it.photoUrl)
        }
        if (cachedCatches.isNotEmpty()) {
            return cachedCatches
        }

        val remoteCatches = httpClient.get("http://10.0.2.2:8080/catches").body<List<CatchModel>>()
        remoteCatches.forEach {
            queries.insert(it.id, it.species, it.weight, it.length, it.photoUrl)
        }
        return remoteCatches
    }
}
