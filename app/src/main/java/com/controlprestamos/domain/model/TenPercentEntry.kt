package com.controlprestamos.domain.model

data class TenPercentEntry(
    val id: Long,
    val date: String,
    val clientName: String,
    val referredTo: String,
    val loanAmountUsd: Double,
    val percent: Double,
    val commissionUsd: Double,
    val status: String,
    val notes: String
)
