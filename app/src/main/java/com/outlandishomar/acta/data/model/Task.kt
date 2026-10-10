package com.outlandishomar.acta.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val date: Long,                  // epoch millis (midnight of the day)
    val isCompleted: Boolean = false,
    val isHighlighted: Boolean = false,  // pinned / important
    val iconName: String? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val taskTimeHour: Int? = null,       // 0–23, null = no time set
    val taskTimeMinute: Int? = null,     // 0–59
    val repeatMode: String = "ONCE",     // ONCE, DAILY, WEEKLY, MONTHLY
    val repeatInterval: Int = 1,         // every N days/weeks/months
    val repeatDays: String = "",         // comma-separated day values for weekly (e.g. "1,5")
    val endDate: Long? = null,           // optional end date for repeating tasks (epoch millis)
    val alertEnabled: Boolean = false,   // true if notification alert is enabled for the primary task time
    val additionalAlerts: String = ""    // delimited string format: hour:minute:isEnabled;hour:minute:isEnabled
)

data class CustomAlert(
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean
) {
    fun toSerializedString() = "$hour:$minute:${if (isEnabled) 1 else 0}"
    
    companion object {
        fun fromString(str: String): CustomAlert? {
            val parts = str.split(":")
            if (parts.size == 3) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val e = parts[2] == "1"
                if (h != null && m != null) {
                    return CustomAlert(h, m, e)
                }
            }
            return null
        }
        
        fun parseList(str: String): List<CustomAlert> {
            if (str.isBlank()) return emptyList()
            return str.split(";").mapNotNull { fromString(it) }
        }
        
        fun serializeList(list: List<CustomAlert>): String {
            return list.joinToString(";") { it.toSerializedString() }
        }
    }
}
