package com.controlprestamos.domain.model

data class SarmEntry(
    val id: Long,
    val date: String,
    val clientName: String,
    val amountUsd: Double,
    val status: String,
    val notes: String
)
