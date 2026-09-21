package io.github.meko123456.tsvima.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.meko123456.tsvima.MainActivity
import io.github.meko123456.tsvima.data.ForecastCache
import io.github.meko123456.tsvima.data.GoOutScore
import io.github.meko123456.tsvima.data.Upcoming
import java.time.LocalDateTime

/** Home-screen widget: the last cached go-out score + next-rain line, at a glance. */
class RainWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cached = ForecastCache(context).read()
        val place: String
        val scoreText: String
        val rain: String
        if (cached != null) {
            val upcoming = Upcoming.hoursAheadNow(cached.forecast.hourly, cached.forecast.utcOffsetSeconds)
            place = cached.place
            scoreText = GoOutScore.score(upcoming).toString()
            val next = Upcoming.nextRain(upcoming)
            rain = if (next == null) "No rain soon ☀️" else "Rain ~${next.time.takeLast(5)}"
        } else {
            place = "Tsvima"
            scoreText = "—"
            rain = "Open to load forecast"
        }

        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(place, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium))
                Text(scoreText, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold))
                Text(rain, style = TextStyle(fontSize = 12.sp))
            }
        }
    }
}

class RainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RainWidget()
}
