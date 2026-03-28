package com.controlprestamos.data

import android.content.Context
import com.controlprestamos.data.db.AppDatabase
import com.controlprestamos.data.repository.LoanRepository
import com.controlprestamos.data.repository.LoanRepositoryImpl
import com.controlprestamos.security.CryptoManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.getInstance(appContext) }
    private val cryptoManager by lazy { CryptoManager() }

    val loanRepository: LoanRepository by lazy {
        LoanRepositoryImpl(
            loanDao = database.loanDao(),
            paymentDao = database.paymentDao(),
            blacklistDao = database.blacklistDao(),
            cryptoManager = cryptoManager
        )
    }
}