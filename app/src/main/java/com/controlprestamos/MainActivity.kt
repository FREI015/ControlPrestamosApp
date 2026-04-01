package com.controlprestamos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.controlprestamos.app.AppNavGraph
import com.controlprestamos.app.SessionStore
import com.controlprestamos.app.SessionTimeoutManager

class MainActivity : FragmentActivity() {

    private lateinit var timeoutManager: SessionTimeoutManager
    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionStore = SessionStore(this)
        timeoutManager = SessionTimeoutManager(
            onTimeout = {
                sessionStore.setUnlocked(false)
                sessionStore.appendHistory("Bloqueo automático por inactividad")
                runOnUiThread { recreate() }
            }
        )

        setContent {
            val navController = rememberNavController()
            AppNavGraph(
                activity = this,
                navController = navController,
                sessionStore = sessionStore
            )
        }
    }

    override fun onResume() {
        super.onResume()
        timeoutManager.start()
    }

    override fun onPause() {
        super.onPause()
        timeoutManager.stop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        timeoutManager.ping()
    }
}
