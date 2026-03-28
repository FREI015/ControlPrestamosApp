package com.controlprestamos.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("loanId"),
        Index("paymentDateEpochDay"),
        Index("paymentCycleKey"),
        Index("wasReminderMessageSent")
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val amount: Double,
    val paymentDateEpochDay: Long,
    val noteCipher: String,
    val createdAtMillis: Long,

    // Conversión del día
    val exchangeRateUsed: Double? = null,
    val amountVes: Double? = null,

    // Control operativo
    val paymentType: String = "REGULAR",
    val wasReminderMessageSent: Boolean = false,

    // Quincena a la que pertenece el movimiento
    val paymentCycleKey: String = ""
)