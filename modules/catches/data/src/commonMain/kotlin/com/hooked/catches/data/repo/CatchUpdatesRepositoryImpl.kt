package com.hooked.catches.data.repo

import com.hooked.catches.data.live.CatchEnrichmentUpdatesService
import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import com.hooked.catches.domain.repositories.CatchUpdatesRepository
import kotlinx.coroutines.flow.Flow

class CatchUpdatesRepositoryImpl(
    private val service: CatchEnrichmentUpdatesService
) : CatchUpdatesRepository {
    override fun updates(): Flow<CatchEnrichmentUpdate> = service.updatesFlow()
}
