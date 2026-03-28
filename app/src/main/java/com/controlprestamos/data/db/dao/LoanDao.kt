package com.controlprestamos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.controlprestamos.data.entity.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY loanDateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    fun observeById(loanId: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    suspend fun getById(loanId: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("SELECT COUNT(*) FROM loans")
    suspend fun count(): Int
}
