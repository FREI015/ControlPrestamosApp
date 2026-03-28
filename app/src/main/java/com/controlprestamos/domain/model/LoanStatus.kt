package com.controlprestamos.domain.model

enum class LoanStatus {
    ACTIVE,
    COLLECTED,
    LOST;

    companion object {
        fun fromStorage(value: String): LoanStatus {
            return entries.firstOrNull { it.name == value } ?: ACTIVE
        }
    }
}
