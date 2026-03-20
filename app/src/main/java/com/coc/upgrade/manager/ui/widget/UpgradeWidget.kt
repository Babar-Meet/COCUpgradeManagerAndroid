package com.coc.upgrade.manager.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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

class UpgradeWidget : GlanceAppWidget() {

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
            val state = currentState<Preferences>()
            val expandedTh = state[stringPreferencesKey("expanded_th")] ?: ""
            
            val now = System.currentTimeMillis()
            
            // Group all tasks by TH
            val thGroups = tasks.groupBy { it.th }
                .toList()
                .sortedBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }

            UpgradeWidgetContent(context, thGroups, expandedTh, now)
        }
    }
}

@Composable
private fun UpgradeWidgetContent(
    context: Context,
    thGroups: List<Pair<String, List<UpgradeTask>>>,
    expandedTh: String,
    now: Long
) {
    val bgColor = ColorProvider(R.color.bg_dark)
    val primaryColor = ColorProvider(R.color.primary)
    val successColor = ColorProvider(R.color.success)
    val textColor = ColorProvider(R.color.text)
    val secondaryTextColor = ColorProvider(R.color.text_secondary)
    val cardColor = ColorProvider(R.color.bg_card)
    val darkColor = ColorProvider(R.color.bg_dark)

    val autoProcessKey = ActionParameters.Key<Boolean>("auto_process")
    val filterThKey = ActionParameters.Key<String>("filter_th")
    val thKey = ActionParameters.Key<String>("th")

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
                text = "⚔️ Upgrades",
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

        if (thGroups.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No upgrades found", style = TextStyle(color = textColor, fontSize = 12.sp))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(thGroups) { (th, allTasksInGroup) ->
                    val isExpanded = expandedTh == th
                    
                    val completedTasks = allTasksInGroup.filter { it.done || it.endTime <= now }
                        .sortedByDescending { it.endTime }
                    val activeTasks = allTasksInGroup.filter { !it.done && it.endTime > now }
                        .sortedBy { it.endTime }
                    
                    val displayTasks = completedTasks + activeTasks
                    if (displayTasks.isEmpty()) return@items

                    val nextTask = displayTasks.first()
                    val isGroupCompleted = activeTasks.isEmpty()

                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(cardColor)
                            .padding(8.dp)
                            .clickable(actionRunCallback<ExpandThCallback>(
                                actionParametersOf(thKey to if (isExpanded) "" else th)
                            ))
                    ) {
                        val tagSuffix = if (nextTask.tag.isNotEmpty()) "(${nextTask.tag})" else ""
                        val statusText = if (isGroupCompleted) "✅" else formatDuration(nextTask.endTime - now)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Th$th$tagSuffix - $statusText",
                                style = TextStyle(
                                    color = if (isGroupCompleted) secondaryTextColor else primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isGroupCompleted) 14.sp else 12.sp
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            
                            Image(
                                provider = ImageProvider(if (isExpanded) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float),
                                contentDescription = null,
                                modifier = GlanceModifier.size(16.dp)
                            )
                        }
                        
                        if (isExpanded) {
                            displayTasks.forEach { task ->
                                val isTaskDone = task.done || task.endTime <= now
                                Row(
                                    modifier = GlanceModifier
                                        .padding(top = 4.dp, start = 8.dp)
                                        .clickable(actionStartActivity<MainActivity>(
                                            parameters = actionParametersOf(filterThKey to th)
                                        ))
                                ) {
                                    Text(
                                        task.upgrade + (if (task.level != null) " Lvl ${task.level}" else ""),
                                        style = TextStyle(
                                            color = if (isTaskDone) secondaryTextColor else textColor,
                                            fontSize = 11.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Text(
                                        if (isTaskDone) "✅" else formatDuration(task.endTime - now),
                                        style = TextStyle(
                                            color = successColor, 
                                            fontSize = if (isTaskDone) 14.sp else 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                nextTask.upgrade + (if (nextTask.level != null) " Lvl ${nextTask.level}" else ""),
                                style = TextStyle(
                                    color = if (isGroupCompleted) secondaryTextColor else textColor,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

class ExpandThCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val thKey = ActionParameters.Key<String>("th")
        val th = parameters[thKey] ?: ""
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("expanded_th")] = th
            }
        }
        UpgradeWidget().update(context, glanceId)
    }
}

class RefreshAllCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        UpgradeWidget().updateAll(context)
        StatsWidget().updateAll(context)
        ListWidget().updateAll(context)
    }
}

private fun formatDuration(millis: Long): String {
    val remaining = max(0L, millis)
    val totalSeconds = remaining / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return when {
        days > 0 -> "${days}D ${hours}H"
        hours > 0 -> "${hours}H ${minutes}M"
        else -> "${minutes}M"
    }
}

class UpgradeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpgradeWidget()
}
