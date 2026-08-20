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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.tsvima.data.DeviceLocation
import io.github.meko123456.tsvima.ui.HomeViewModel
import io.github.meko123456.tsvima.ui.HomeScreen
import io.github.meko123456.tsvima.ui.PlaceSearchDialog
import io.github.meko123456.tsvima.ui.theme.TsvimaTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TsvimaTheme {
                val vm: HomeViewModel = viewModel()
                val state by vm.state.collectAsState()
                val context = LocalContext.current
                var showSearch by remember { mutableStateOf(false) }

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

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Tsvima") },
                            actions = {
                                IconButton(onClick = { vm.clearSearch(); showSearch = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Find a city")
                                }
                            },
                        )
                    },
                ) { padding ->
                    HomeScreen(
                        state = state,
                        refreshing = vm.refreshing,
                        onRefresh = vm::refresh,
                        onRetry = vm::refresh,
                        modifier = Modifier.padding(padding),
                    )
                }

                if (showSearch) {
                    PlaceSearchDialog(
                        searching = vm.searching,
                        results = vm.searchResults,
                        onSearch = vm::search,
                        onPick = { vm.pickPlace(it); showSearch = false },
                        onDismiss = { showSearch = false },
                    )
                }
            }
        }
    }
}
