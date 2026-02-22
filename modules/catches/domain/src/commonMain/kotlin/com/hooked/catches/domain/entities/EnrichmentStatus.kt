package com.hooked.catches.domain.entities

enum class EnrichmentStatus {
    Pending,
    Completed,
    Failed;

    companion object {
        fun fromBoolean(value: Boolean?): EnrichmentStatus = when (value) {
            true -> Completed
            false -> Failed
            null -> Pending
        }

        fun toBoolean(status: EnrichmentStatus): Boolean? = when (status) {
            Pending -> null
            Completed -> true
            Failed -> false
        }
    }
}
