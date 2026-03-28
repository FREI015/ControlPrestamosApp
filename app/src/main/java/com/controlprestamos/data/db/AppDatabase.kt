package com.controlprestamos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.controlprestamos.data.db.dao.BlacklistDao
import com.controlprestamos.data.db.dao.LoanDao
import com.controlprestamos.data.db.dao.PaymentDao
import com.controlprestamos.data.entity.BlacklistEntity
import com.controlprestamos.data.entity.LoanEntity
import com.controlprestamos.data.entity.PaymentEntity

@Database(
    entities = [
        LoanEntity::class,
        PaymentEntity::class,
        BlacklistEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        const val DATABASE_NAME = "control_prestamos.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}