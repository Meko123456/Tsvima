package io.github.meko123456.tsvima.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(state: HomeUi, modifier: Modifier = Modifier) {
    when (state) {
        is HomeUi.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUi.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
        is HomeUi.Ready -> Ready(state, modifier)
    }
}

@Composable
private fun Ready(state: HomeUi.Ready, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(state.place, style = MaterialTheme.typography.titleMedium)

        Text(
            "${state.score}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = scoreColor(state.score),
        )
        Text("out of 100 · ${state.verdict}", style = MaterialTheme.typography.bodyMedium)

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                state.nextRain,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        Text(
            "Next hours",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.hours) { h -> HourCell(h) }
        }
    }
}

@Composable
private fun HourCell(h: HourRow) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(72.dp),
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(h.label, style = MaterialTheme.typography.labelMedium)
            Text(if (h.prob > 0) "💧${h.prob}%" else "—", style = MaterialTheme.typography.bodySmall)
            Text("${h.tempC.toInt()}°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun scoreColor(score: Int) = when {
    score >= 60 -> MaterialTheme.colorScheme.primary
    score >= 40 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
