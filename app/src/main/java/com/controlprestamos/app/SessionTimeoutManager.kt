package com.controlprestamos.app

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
