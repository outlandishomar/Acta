package com.outlandishomar.acta.data.util

import com.outlandishomar.acta.data.model.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

object RepeatTaskCalculator {

    fun doesTaskOccurOnDate(task: Task, startDate: LocalDate, targetDate: LocalDate): Boolean {
        if (targetDate.isBefore(startDate)) return false
        
        // Check if the target date is past the end date
        if (task.endDate != null) {
            val endDate = Instant.ofEpochMilli(task.endDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (targetDate.isAfter(endDate)) return false
        }
        
        return when (task.repeatMode) {
            "DAILY" -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, targetDate)
                daysBetween % task.repeatInterval == 0L
            }
            "WEEKLY" -> {
                val selectedDays = task.repeatDays
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .map { DayOfWeek.of(it) }
                    .toSet()
                    .ifEmpty { setOf(startDate.dayOfWeek) }

                val targetDow = targetDate.dayOfWeek
                if (!selectedDays.contains(targetDow)) {
                    return false
                }

                val startWeekMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val targetWeekMonday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                
                val weeksBetween = ChronoUnit.WEEKS.between(startWeekMonday, targetWeekMonday)
                weeksBetween % task.repeatInterval == 0L
            }
            "MONTHLY" -> {
                val monthsBetween = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), targetDate.withDayOfMonth(1))
                if (monthsBetween % task.repeatInterval != 0L) return false
                
                val expectedDay = min(startDate.dayOfMonth, targetDate.lengthOfMonth())
                targetDate.dayOfMonth == expectedDay
            }
            else -> false
        }
    }
}
