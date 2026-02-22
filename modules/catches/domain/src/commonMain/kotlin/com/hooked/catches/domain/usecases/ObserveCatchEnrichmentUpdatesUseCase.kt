package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import com.hooked.catches.domain.repositories.CatchUpdatesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCatchEnrichmentUpdatesUseCase(
    private val repository: CatchUpdatesRepository
) {
    operator fun invoke(): Flow<CatchEnrichmentUpdate> = repository.updates()
}
