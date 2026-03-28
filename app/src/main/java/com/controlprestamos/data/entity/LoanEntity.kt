package com.controlprestamos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loans",
    indices = [
        Index("customerName"),
        Index("status"),
        Index("loanDateEpochDay"),
        Index("dueDateEpochDay"),
        Index("idNumberHash"),
        Index("phoneHash"),
        Index("originCycleKey"),
        Index("closedCycleKey"),
        Index("isArchived"),
        Index("lastReminderDateEpochDay")
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String,
    val phoneCipher: String,
    val phoneHash: String,
    val idNumberCipher: String,
    val idNumberHash: String,

    val principalAmount: Double,
    val interestRate: Double,
    val profitAmount: Double,
    val totalToRepay: Double,

    val loanDateEpochDay: Long,
    val dueDateEpochDay: Long,
    val status: String,
    val observationsCipher: String,

    val createdAtMillis: Long,
    val updatedAtMillis: Long,

    // Cierre / historial
    val closedAtEpochDay: Long? = null,
    val isArchived: Boolean = false,
    val archivedAtEpochDay: Long? = null,

    // Quincenas
    val originCycleKey: String = "",
    val closedCycleKey: String? = null,

    // Recordatorios / cobranza
    val reminderSent: Boolean = false,
    val lastReminderDateEpochDay: Long? = null,

    // Conversión y cobro del día
    val manualExchangeRate: Double? = null,
    val calculatedAmountVes: Double? = null,

    // Mora / recargo
    val lateFeePercent: Double = 0.0,
    val lateFeeFixedAmount: Double = 0.0,

    // Fecha real de cobro
    val collectedAtEpochDay: Long? = null
)