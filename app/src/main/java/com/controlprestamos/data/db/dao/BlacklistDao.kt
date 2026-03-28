package com.controlprestamos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.controlprestamos.data.entity.BlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist WHERE active = 1 ORDER BY addedDateEpochDay DESC, id DESC")
    fun observeAllActive(): Flow<List<BlacklistEntity>>

    @Query("SELECT * FROM blacklist WHERE active = 1")
    suspend fun getAllActive(): List<BlacklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlacklistEntity): Long

    @Query("UPDATE blacklist SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("SELECT COUNT(*) FROM blacklist")
    suspend fun count(): Int
}
