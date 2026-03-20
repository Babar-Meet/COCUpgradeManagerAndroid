package com.coc.upgrade.manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upgrade_tasks")
data class UpgradeTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tag: String = "",
    val th: String = "",
    val upgrade: String,
    val level: Int? = null,
    val endTime: Long,
    val done: Boolean = false,
    val code: String? = null,
    val category: String? = null
) {
    companion object {
        fun fromJson(json: Map<String, Any>): UpgradeTask {
            return UpgradeTask(
                tag = json["tag"] as? String ?: "",
                th = json["th"] as? String ?: "",
                upgrade = json["upgrade"] as? String ?: "",
                level = (json["level"] as? Number)?.toInt(),
                endTime = (json["endTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                done = json["done"] as? Boolean ?: false,
                code = json["code"] as? String,
                category = json["category"] as? String
            )
        }
    }

    fun toJson(): Map<String, Any?> = mapOf(
        "tag" to tag,
        "th" to th,
        "upgrade" to upgrade,
        "level" to level,
        "endTime" to endTime,
        "done" to done,
        "code" to code,
        "category" to category
    )
}
