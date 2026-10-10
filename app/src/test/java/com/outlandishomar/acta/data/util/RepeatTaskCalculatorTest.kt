package com.outlandishomar.acta.data.util

import com.outlandishomar.acta.data.model.Task
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant

class RepeatTaskCalculatorTest {

    private fun createTask(
        repeatMode: String,
        repeatInterval: Int = 1,
        repeatDays: String = "",
        endDateMillis: Long? = null
    ): Task {
        return Task(
            title = "Test",
            description = "",
            date = 0L,
            repeatMode = repeatMode,
            repeatInterval = repeatInterval,
            repeatDays = repeatDays,
            endDate = endDateMillis,
            alertEnabled = false
        )
    }

    private fun LocalDate.toEpochMilli(): Long {
        return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @Test
    fun testTargetDateBeforeStartDate_ReturnsFalse() {
        val task = createTask("DAILY")
        val startDate = LocalDate.of(2023, 1, 10)
        val targetDate = LocalDate.of(2023, 1, 9)
        
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, targetDate))
    }

    @Test
    fun testTargetDateAfterEndDate_ReturnsFalse() {
        val endDate = LocalDate.of(2023, 1, 15)
        val task = createTask("DAILY", endDateMillis = endDate.toEpochMilli())
        val startDate = LocalDate.of(2023, 1, 10)
        val targetDate = LocalDate.of(2023, 1, 16)
        
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, targetDate))
    }

    @Test
    fun testDailyRepeat_EveryDay() {
        val task = createTask("DAILY")
        val startDate = LocalDate.of(2023, 1, 10)
        
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 10)))
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 11)))
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 20)))
    }

    @Test
    fun testDailyRepeat_Every3Days() {
        val task = createTask("DAILY", repeatInterval = 3)
        val startDate = LocalDate.of(2023, 1, 10)
        
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 10)))
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 11)))
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 12)))
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 13)))
    }

    @Test
    fun testWeeklyRepeat_SpecificDays() {
        // Monday(1) and Wednesday(3)
        val task = createTask("WEEKLY", repeatDays = "1,3")
        val startDate = LocalDate.of(2023, 1, 9) // Monday
        
        // Same week
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 9))) // Monday
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 10))) // Tuesday
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 11))) // Wednesday
        
        // Next week
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 16))) // Monday
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 18))) // Wednesday
    }

    @Test
    fun testWeeklyRepeat_Every2Weeks() {
        val task = createTask("WEEKLY", repeatInterval = 2, repeatDays = "1") // Every 2 weeks on Monday
        val startDate = LocalDate.of(2023, 1, 9) // Monday
        
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 9))) // Week 1
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 16))) // Week 2
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 23))) // Week 3
    }

    @Test
    fun testMonthlyRepeat_EveryMonth() {
        val task = createTask("MONTHLY")
        val startDate = LocalDate.of(2023, 1, 15)
        
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 15)))
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 2, 15)))
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 3, 15)))
        
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 2, 16)))
    }
    
    @Test
    fun testMonthlyRepeat_MonthRolloverEdgeCase() {
        val task = createTask("MONTHLY")
        // Start on Jan 31
        val startDate = LocalDate.of(2023, 1, 31)
        
        // Feb has only 28 days
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 2, 28)))
        // April has 30 days
        assertTrue(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 4, 30)))
    }
    
    @Test
    fun testOnceRepeat_ReturnsFalse() {
        val task = createTask("ONCE")
        val startDate = LocalDate.of(2023, 1, 10)
        
        // Repeat mode ONCE is handled manually in ViewModel/Repository, this function should return false
        assertFalse(RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, LocalDate.of(2023, 1, 10)))
    }
}
