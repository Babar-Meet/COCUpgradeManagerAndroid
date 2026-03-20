package com.coc.upgrade.manager.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.coc.upgrade.manager.R
import com.coc.upgrade.manager.data.repository.UpgradeRepository
import com.coc.upgrade.manager.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class StatsWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun repository(): UpgradeRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).repository()

        provideContent {
            val total by repository.totalCount.collectAsState(initial = 0)
            val completed by repository.completedCount.collectAsState(initial = 0)
            val active by repository.activeCount.collectAsState(initial = 0)

            StatsWidgetContent(total, completed, active)
        }
    }
}

@Composable
private fun StatsWidgetContent(total: Int, completed: Int, active: Int) {
    val bgColor = ColorProvider(R.color.bg_dark)
    val primaryColor = ColorProvider(R.color.primary)
    val textColor = ColorProvider(android.R.color.white)
    val cardColor = ColorProvider(R.color.bg_card)
    val successColor = ColorProvider(R.color.success)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
            .appWidgetBackground()
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Stats",
                style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            // Refresh Button
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_rotate),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(actionRunCallback<RefreshAllCallback>())
            )
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatBox("TOTAL", total.toString(), primaryColor, textColor, cardColor)
            Spacer(modifier = GlanceModifier.width(8.dp))
            StatBox("DONE", completed.toString(), successColor, textColor, cardColor)
            Spacer(modifier = GlanceModifier.width(8.dp))
            StatBox("ACTIVE", active.toString(), primaryColor, textColor, cardColor)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, accent: ColorProvider, textColor: ColorProvider, bg: ColorProvider) {
    Column(
        modifier = GlanceModifier
            .width(60.dp)
            .background(bg)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = TextStyle(color = textColor, fontSize = 8.sp)
        )
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}
