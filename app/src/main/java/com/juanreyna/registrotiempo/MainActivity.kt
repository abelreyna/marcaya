package com.juanreyna.registrotiempo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.juanreyna.registrotiempo.ui.navigation.AppNav
import com.juanreyna.registrotiempo.ui.theme.RegistroTiempoTheme
import com.juanreyna.registrotiempo.util.InAppUpdates
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var updates: InAppUpdates
    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updates = InAppUpdates(this)
        updates.verificar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { RegistroTiempoTheme { AppNav() } }
    }

    override fun onResume() {
        super.onResume()
        updates.reanudar()
    }
}
