package io.github.meko123456.tsvima.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
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
import io.github.meko123456.tsvima.ui.hourLabel
import io.github.meko123456.tsvima.ui.widgetRainLine

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
            rain = widgetRainLine(upcoming, DateFormat.is24HourFormat(context))
        } else {
            place = "Tsvima"
            scoreText = "—"
            rain = "Open to load forecast"
        }

        provideContent { RainCard(context, place, scoreText, rain) }
    }

    /**
     * What the widget picker shows on Android 15 and later: the card itself, with a made-up
     * forecast, instead of the app icon in a box. Drawn by the same composable as the widget, so
     * the two can't drift apart. [WidgetPreviews] hands it to the system.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val rain = "Rain ~${hourLabel("2026-01-01T17:00", DateFormat.is24HourFormat(context))}"
        provideContent { RainCard(context, place = "Tbilisi", scoreText = "86", rain = rain) }
    }
}

@Composable
private fun RainCard(context: Context, place: String, scoreText: String, rain: String) {
    GlanceTheme {
        // Glance draws nothing behind a widget unless asked, and its text is a flat black: over a
        // dark wallpaper, or the dark half of one, the score all but disappeared. The theme's
        // widget background and ink follow the wallpaper and dark mode.
        val ink = GlanceTheme.colors.onSurface
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .padding(12.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(place, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink))
            Text(scoreText, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ink))
            Text(rain, style = TextStyle(fontSize = 12.sp, color = ink))
        }
    }
}

class RainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RainWidget()
}
