package com.controlprestamos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.controlprestamos.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY paymentDateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDateEpochDay DESC, id DESC")
    fun observeByLoanId(loanId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDateEpochDay DESC, id DESC")
    suspend fun getByLoanId(loanId: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long
}
