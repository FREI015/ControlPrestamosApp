package com.controlprestamos.app

import com.controlprestamos.features.backup.*
import com.controlprestamos.features.security.*
import com.controlprestamos.features.settings.*
import com.controlprestamos.features.dashboard.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.core.format.*
import com.controlprestamos.core.validation.*
import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.os.Handler
import android.os.Looper

class SessionTimeoutManager(
    private val onTimeout: () -> Unit,
    private val timeoutMillis: Long = 10 * 60 * 1000L
) {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    private val timeoutRunnable = Runnable {
        if (started) {
            onTimeout()
        }
    }

    fun start() {
        started = true
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, timeoutMillis)
    }

    fun stop() {
        started = false
        handler.removeCallbacks(timeoutRunnable)
    }

    fun ping() {
        if (started) {
            handler.removeCallbacks(timeoutRunnable)
            handler.postDelayed(timeoutRunnable, timeoutMillis)
        }
    }
}
