package com.hooked.catches.domain.repositories

import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import kotlinx.coroutines.flow.Flow

interface CatchUpdatesRepository {
    fun updates(): Flow<CatchEnrichmentUpdate>
}
