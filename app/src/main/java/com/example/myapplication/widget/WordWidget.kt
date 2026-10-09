package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.repository.AppRepository

class WordWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = AppRepository.getInstance(context)
            val randomWord = repository.getRandomWordForWidget()

            val views = RemoteViews(context.packageName, R.layout.word_widget_layout)

            if (randomWord != null) {
                views.setTextViewText(R.id.widget_english, randomWord.english)
                views.setTextViewText(R.id.widget_kazakh, randomWord.kazakh)
                views.setTextViewText(
                    R.id.widget_example,
                    if (randomWord.exampleSentence.isNotBlank()) "“${randomWord.exampleSentence}”" else ""
                )
            } else {
                views.setTextViewText(R.id.widget_english, "Hello")
                views.setTextViewText(R.id.widget_kazakh, "Сәлем")
                views.setTextViewText(R.id.widget_example, "Сөздер қосыңыз!")
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
