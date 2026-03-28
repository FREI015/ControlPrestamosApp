package com.controlprestamos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blacklist",
    indices = [Index("idNumberHash"), Index("phoneHash")]
)
data class BlacklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String,
    val phoneCipher: String,
    val phoneHash: String,
    val idNumberCipher: String,
    val idNumberHash: String,
    val reasonCipher: String,
    val addedDateEpochDay: Long,
    val active: Boolean = true
)
