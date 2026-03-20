package com.coc.upgrade.manager.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.coc.upgrade.manager.R
import com.coc.upgrade.manager.data.model.UpgradeTask
import com.coc.upgrade.manager.data.repository.UpgradeRepository
import com.coc.upgrade.manager.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.max

class ListWidget : GlanceAppWidget() {

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
            val tasks by repository.allTasks.collectAsState(initial = emptyList())
            val now = System.currentTimeMillis()
            val activeTasks = tasks.filter { !it.done && it.endTime > now }
                .sortedBy { it.endTime }

            ListWidgetContent(activeTasks, now)
        }
    }
}

@Composable
private fun ListWidgetContent(tasks: List<UpgradeTask>, now: Long) {
    val bgColor = ColorProvider(R.color.bg_dark)
    val primaryColor = ColorProvider(R.color.primary)
    val successColor = ColorProvider(R.color.success)
    val textColor = ColorProvider(android.R.color.white)
    val cardColor = ColorProvider(R.color.bg_card)
    val darkColor = ColorProvider(R.color.bg_dark)
    val filterThKey = ActionParameters.Key<String>("filter_th")
    val autoProcessKey = ActionParameters.Key<Boolean>("auto_process")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(8.dp)
            .appWidgetBackground()
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏳ Timeline",
                style = TextStyle(color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            // Refresh Button
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_rotate),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<RefreshAllCallback>())
            )
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            // Process JSON Button
            Button(
                text = "PROCESS JSON",
                onClick = actionStartActivity<MainActivity>(
                    parameters = actionParametersOf(autoProcessKey to true)
                ),
                modifier = GlanceModifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = primaryColor,
                    contentColor = darkColor
                ),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }

        if (tasks.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All builders free!", style = TextStyle(color = textColor, fontSize = 12.sp))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(tasks) { task ->
                    val tagSuffix = if (task.tag.isNotEmpty()) " (${task.tag})" else ""
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(cardColor)
                            .padding(8.dp)
                            .clickable(actionStartActivity<MainActivity>(
                                parameters = actionParametersOf(filterThKey to task.th)
                            )),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "TH${task.th}$tagSuffix ${task.upgrade}",
                                style = TextStyle(color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            if (task.level != null) {
                                Text(
                                    text = "Level ${task.level}",
                                    style = TextStyle(color = primaryColor, fontSize = 9.sp)
                                )
                            }
                        }
                        Text(
                            text = formatDurationFull(task.endTime - now),
                            style = TextStyle(color = successColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationFull(millis: Long): String {
    val remaining = max(0L, millis)
    val totalSeconds = remaining / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return buildString {
        if (days > 0) append("${days}D ")
        if (hours > 0) append("${hours}H ")
        if (minutes > 0 || (days == 0L && hours == 0L)) append("${minutes}M ")
    }.trim()
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListWidget()
}
