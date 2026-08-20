package io.github.meko123456.tsvima

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.tsvima.data.DeviceLocation
import io.github.meko123456.tsvima.ui.HomeViewModel
import io.github.meko123456.tsvima.ui.HomeScreen
import io.github.meko123456.tsvima.ui.theme.TsvimaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TsvimaTheme {
                val vm: HomeViewModel = viewModel()
                val state by vm.state.collectAsState()
                val context = LocalContext.current

                // Load for the device location if we can get it; otherwise a sensible default.
                fun loadForDevice() {
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    val here = if (coarse == PackageManager.PERMISSION_GRANTED) DeviceLocation(context).lastKnown() else null
                    if (here != null) vm.load(here.first, here.second, "Current location")
                    else vm.load(41.7, 44.8, "Tbilisi (default)")
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { loadForDevice() }

                LaunchedEffect(Unit) {
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (coarse == PackageManager.PERMISSION_GRANTED) loadForDevice()
                    else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }

                Scaffold { padding ->
                    HomeScreen(state, Modifier.padding(padding))
                }
            }
        }
    }
}
