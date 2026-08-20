package io.github.meko123456.tsvima

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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

                // Default place until device location lands (#4). Tbilisi.
                LaunchedEffect(Unit) { vm.load(41.7, 44.8, "Tbilisi") }

                Scaffold { padding ->
                    HomeScreen(state, Modifier.padding(padding))
                }
            }
        }
    }
}
