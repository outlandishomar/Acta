package com.outlandishomar.acta.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.Divider
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.SfProText
import com.outlandishomar.acta.ui.theme.calculateLuminance
import com.outlandishomar.acta.data.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ─── Constants ──────────────────────────────────────────────────────────────────

/** Week pager: ±50 000 weeks ≈ ±961 years. */
private const val WEEK_PAGE_COUNT = 100_001
private const val WEEK_INDEX_TODAY = 50_000

/** Month pager: ±5 000 months ≈ ±416 years. */
private const val MONTH_PAGE_COUNT = 10_001
private const val MONTH_INDEX_TODAY = 5_000

// ─── Week helpers ───────────────────────────────────────────────────────────────

private fun startOfWeek(date: LocalDate): LocalDate {
    val currentDay = date.dayOfWeek.value
    val firstDay = AppSettings.firstDayOfWeek.value
    val offset = (currentDay - firstDay + 7) % 7
    return date.minusDays(offset.toLong())
}

private fun weekPageToStartOfWeek(page: Int, todayStart: LocalDate): LocalDate {
    val weekOffset = (page - WEEK_INDEX_TODAY).toLong()
    return todayStart.plusWeeks(weekOffset)
}

private fun dateToWeekPage(date: LocalDate, todayStart: LocalDate): Int {
    val weeksBetween = ChronoUnit.DAYS.between(todayStart, startOfWeek(date)) / 7
    return (WEEK_INDEX_TODAY + weeksBetween).toInt()
}

// ─── Month helpers ──────────────────────────────────────────────────────────────

private fun monthPageToYearMonth(page: Int, todayYm: YearMonth): YearMonth {
    val offset = (page - MONTH_INDEX_TODAY).toLong()
    return todayYm.plusMonths(offset)
}

private fun yearMonthToMonthPage(ym: YearMonth, todayYm: YearMonth): Int {
    val months = ChronoUnit.MONTHS.between(todayYm, ym)
    return (MONTH_INDEX_TODAY + months).toInt()
}

// ─── Main Composable ────────────────────────────────────────────────────────────

@Composable
fun CalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    hasTasks: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val firstDayOfWeek = AppSettings.firstDayOfWeek
    val todayStart = remember(firstDayOfWeek) { startOfWeek(today) }
    val todayYm = remember { YearMonth.from(today) }

    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Vertical drag gesture area wraps the animated content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag > 60f && !isExpanded) {
                                isExpanded = true
                            } else if (totalDrag < -60f && isExpanded) {
                                isExpanded = false
                            }
                            totalDrag = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(300)
                    ).togetherWith(
                        fadeOut(animationSpec = tween(150))
                    ).using(
                        SizeTransform(
                            clip = true,
                            sizeAnimationSpec = { _, _ ->
                                spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 300f
                                )
                            }
                        )
                    )
                },
                label = "calendar_expansion"
            ) { expanded ->
                if (expanded) {
                    MonthView(
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected,
                        today = today,
                        todayYm = todayYm,
                        firstDayOfWeek = firstDayOfWeek,
                        hasTasks = hasTasks
                    )
                } else {
                    WeekView(
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected,
                        today = today,
                        todayStart = todayStart,
                        firstDayOfWeek = firstDayOfWeek,
                        hasTasks = hasTasks
                    )
                }
            }
        }

        // Bottom divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Divider)
        )
    }
}

// ─── Week View (existing pager logic) ───────────────────────────────────────────

@Composable
private fun WeekView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    today: LocalDate,
    todayStart: LocalDate,
    firstDayOfWeek: DayOfWeek,
    hasTasks: (LocalDate) -> Boolean
) {
    val initialPage = remember(firstDayOfWeek) { dateToWeekPage(selectedDate, todayStart) }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { WEEK_PAGE_COUNT }
    )

    LaunchedEffect(firstDayOfWeek) {
        val targetPage = dateToWeekPage(selectedDate, todayStart)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // Sync: selectedDate → pager
    LaunchedEffect(selectedDate) {
        val targetPage = dateToWeekPage(selectedDate, todayStart)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync: pager → selectedDate
    LaunchedEffect(pagerState.settledPage) {
        val settledStart = weekPageToStartOfWeek(pagerState.settledPage, todayStart)
        val currentStart = startOfWeek(selectedDate)

        if (settledStart != currentStart) {
            val dayOffset = ChronoUnit.DAYS.between(currentStart, selectedDate)
            val newDate = settledStart.plusDays(dayOffset)
            onDateSelected(newDate)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        beyondViewportPageCount = 1,
        key = { it }
    ) { page ->
        val startOfWeekDay = weekPageToStartOfWeek(page, todayStart)
        val days = (0..6).map { startOfWeekDay.plusDays(it.toLong()) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { date ->
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    isCurrentMonth = true,
                    compact = true,
                    hasTasks = hasTasks(date),
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

// ─── Month View ─────────────────────────────────────────────────────────────────

@Composable
private fun MonthView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    today: LocalDate,
    todayYm: YearMonth,
    firstDayOfWeek: DayOfWeek,
    hasTasks: (LocalDate) -> Boolean
) {
    val initialPage = remember { yearMonthToMonthPage(YearMonth.from(selectedDate), todayYm) }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { MONTH_PAGE_COUNT }
    )

    // Sync: selectedDate → pager
    LaunchedEffect(selectedDate) {
        val targetPage = yearMonthToMonthPage(YearMonth.from(selectedDate), todayYm)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync: pager settled → update selectedDate month if user swiped
    LaunchedEffect(pagerState.settledPage) {
        val settledYm = monthPageToYearMonth(pagerState.settledPage, todayYm)
        val currentYm = YearMonth.from(selectedDate)
        if (settledYm != currentYm) {
            val newDay = selectedDate.dayOfMonth.coerceAtMost(settledYm.lengthOfMonth())
            onDateSelected(settledYm.atDay(newDay))
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        beyondViewportPageCount = 1,
        key = { it }
    ) { page ->
        val ym = monthPageToYearMonth(page, todayYm)
        MonthGrid(
            yearMonth = ym,
            selectedDate = selectedDate,
            today = today,
            firstDayOfWeek = firstDayOfWeek,
            hasTasks = hasTasks,
            onDateSelected = onDateSelected
        )
    }
}

private data class MonthGridData(
    val startDate: LocalDate,
    val dayHeaders: List<String>
)

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    hasTasks: (LocalDate) -> Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val gridData = remember(yearMonth, firstDayOfWeek) {
        val firstOfMonth = yearMonth.atDay(1)
        val firstDayValue = firstDayOfWeek.value
        val monthStartDow = firstOfMonth.dayOfWeek.value
        val leadingBlanks = (monthStartDow - firstDayValue + 7) % 7
        val startDate = firstOfMonth.minusDays(leadingBlanks.toLong())

        val headers = (0..6).map { i ->
            DayOfWeek.of(((firstDayValue - 1 + i) % 7) + 1)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        MonthGridData(startDate, headers)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Day-of-week header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            gridData.dayHeaders.forEach { header ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = header,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontFamily = SfProText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar grid (always 6 weeks / 42 days)
        for (week in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0..6) {
                    val slotIndex = week * 7 + col
                    val date = gridData.startDate.plusDays(slotIndex.toLong())
                    val isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year

                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isCurrentMonth = isCurrentMonth,
                        compact = false,
                        hasTasks = hasTasks(date),
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }
    }
}

// ─── Shared Day Cell ────────────────────────────────────────────────────────────

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    compact: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit
) {
    val selectedTextColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)
    val numberColor by animateColorAsState(
        targetValue = when {
            !isCurrentMonth -> TextSecondary.copy(alpha = 0.35f)
            isSelected -> selectedTextColor
            isToday -> Blue500
            else -> TextPrimary
        },
        label = "numColor"
    )

    if (compact) {
        // Week view: shows day name + number
        val dayLetter = date.dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(48.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = dayLetter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Blue500 else TextSecondary,
                textAlign = TextAlign.Center,
                fontFamily = SfProText
            )

            Spacer(Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        when {
                            isSelected -> Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Blue500)
                            isToday -> Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, TextPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            else -> Modifier
                        }
                    )
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = numberColor,
                    textAlign = TextAlign.Center
                )
                if (hasTasks) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                    )
                }
            }
        }
    } else {
        // Month view: number only, smaller
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(40.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        when {
                            isSelected && isCurrentMonth -> Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Blue500)
                            isToday && isCurrentMonth -> Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, TextPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            else -> Modifier
                        }
                    )
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    fontWeight = if ((isSelected || isToday) && isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                    color = numberColor,
                    textAlign = TextAlign.Center
                )
                if (hasTasks) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentMonth) Color(0xFFFF3B30) else TextSecondary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}
