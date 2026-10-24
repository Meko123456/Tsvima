package io.github.meko123456.tsvima.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Hands the widget picker the preview [RainWidget.providePreview] draws (Android 15 and later).
 * Without one the picker shows the app icon in a box. Once per install or update: the system
 * rate-limits these calls, and the preview only changes when the app does.
 */
object WidgetPreviews {

    suspend fun publish(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_PUBLISHED_FOR, 0L) == installed) return
        val result = GlanceAppWidgetManager(context).setWidgetPreviews(RainWidgetReceiver::class)
        // Rate-limited: try again the next time the app starts.
        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            prefs.edit().putLong(KEY_PUBLISHED_FOR, installed).apply()
        }
    }

    private const val PREFS = "widget_previews"
    private const val KEY_PUBLISHED_FOR = "published_for_install"
}
