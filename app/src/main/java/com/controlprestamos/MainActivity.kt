package com.controlprestamos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.controlprestamos.ui.navigation.ControlPrestamosRoot
import com.controlprestamos.ui.theme.ControlPrestamosTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ControlPrestamosTheme {
                ControlPrestamosRoot()
            }
        }
    }
}
