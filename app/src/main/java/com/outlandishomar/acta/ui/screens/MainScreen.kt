package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.outlandishomar.acta.data.isTodayUserBirthday
import com.outlandishomar.acta.ui.components.AddTaskDialog
import com.outlandishomar.acta.ui.components.BirthdayConfettiOverlay
import com.outlandishomar.acta.ui.components.TaskTypePickerSheet
import com.outlandishomar.acta.ui.components.UndoFloatingBar
import com.outlandishomar.acta.ui.theme.BottomBarBackground
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.Divider
import com.outlandishomar.acta.ui.theme.Grey50
import com.outlandishomar.acta.ui.theme.Grey100
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.TopBarBackground
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.calculateLuminance
import com.outlandishomar.acta.ui.theme.InterText
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import com.outlandishomar.acta.ui.theme.ThemeState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

private enum class BottomTab(
    val label: String
) {
    Inbox("Inbox"),
    Tasks("Tasks"),
    Settings("Settings")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(openTaskId: Int = -1, onTaskOpened: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(BottomTab.Tasks) }
    val viewModel: TaskViewModel = viewModel()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var targetSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }
    var displayedSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }
    val dismissProgress = remember { Animatable(1f) }
    val subPageScope = rememberCoroutineScope()
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val isInSettingsSubScreen = targetSubPage != null || displayedSubPage != null

    LaunchedEffect(targetSubPage) {
        val target = targetSubPage
        if (target != null) {
            val isFreshOpen = displayedSubPage == null
            displayedSubPage = target
            if (isFreshOpen) {
                dismissProgress.snapTo(1f)
            }
            val remaining = dismissProgress.value.coerceIn(0f, 1f)
            val duration = (remaining * 300f).toInt().coerceAtLeast(1)
            dismissProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            )
        } else if (displayedSubPage != null) {
            val remaining = (1f - dismissProgress.value).coerceIn(0f, 1f)
            val duration = (remaining * 300f).toInt().coerceAtLeast(1)
            dismissProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            )
            displayedSubPage = null
        }
    }

    var showTaskAddDialog by remember { mutableStateOf(false) }
    var showTaskAddHighlightedDialog by remember { mutableStateOf(false) }
    var showInboxAdd by remember { mutableStateOf(false) }

    val isAddOverlayVisible = showTaskAddDialog || showTaskAddHighlightedDialog || showInboxAdd
    val blurModifier = if (isAddOverlayVisible && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else Modifier

    val isDark = ThemeState.isThemeDark
    
    val tasksIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.taskswhite else com.outlandishomar.acta.R.drawable.tasksblack)
    val inboxIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.boxwhite else com.outlandishomar.acta.R.drawable.boxblack)
    val settingsIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.settingswhite else com.outlandishomar.acta.R.drawable.settingsblack)

    var birthdayConfettiKey by remember { mutableIntStateOf(0) }
    var celebratedBirthdayThisSession by remember { mutableStateOf(false) }
    val isBirthdayToday = isTodayUserBirthday()
    LaunchedEffect(isBirthdayToday, isInSettingsSubScreen) {
        if (isBirthdayToday && !celebratedBirthdayThisSession && !isInSettingsSubScreen) {
            birthdayConfettiKey++
            celebratedBirthdayThisSession = true
        }
    }

    BackHandler(enabled = selectedTab == BottomTab.Settings && !isInSettingsSubScreen) {
        selectedTab = BottomTab.Tasks
    }

    PredictiveBackHandler(enabled = targetSubPage != null) { progress ->
        var collectedAny = false
        try {
            progress.collect { backEvent ->
                collectedAny = true
                dismissProgress.snapTo(backEvent.progress.coerceIn(0f, 1f))
            }
            targetSubPage = null
        } catch (e: CancellationException) {
            if (collectedAny) {
                subPageScope.launch {
                    val remaining = dismissProgress.value.coerceIn(0f, 1f)
                    val duration = (remaining * 240f).toInt().coerceAtLeast(1)
                    dismissProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                    )
                }
            }
            throw e
        }
    }

    val density = LocalDensity.current
    val tabEnterOffsetPx = remember(density) { with(density) { 24.dp.roundToPx() } }
    val parallaxOffsetPx = remember(density) { with(density) { 64.dp.roundToPx().toFloat() } }
    val subPageShadowPx = remember(density) { with(density) { 12.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TopBarBackground)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = dismissProgress.value.coerceIn(0f, 1f)
                    if (p < 1f) {
                        val underlyingScale = 1f - 0.06f * (1f - p)
                        translationX = -parallaxOffsetPx * (1f - p)
                        scaleX = underlyingScale
                        scaleY = underlyingScale
                    }
                }
        ) {
        Scaffold(
            containerColor = TopBarBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BottomBarBackground)
                ) {
                    HorizontalDivider(
                        color = Divider.copy(alpha = 0.28f),
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(64.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavTab(
                            label = BottomTab.Inbox.label,
                            painter = inboxIcon,
                            selected = selectedTab == BottomTab.Inbox,
                            onClick = { selectedTab = BottomTab.Inbox },
                            modifier = Modifier.weight(1f)
                        )

                        BottomNavTab(
                            label = BottomTab.Tasks.label,
                            painter = tasksIcon,
                            selected = selectedTab == BottomTab.Tasks,
                            onClick = { selectedTab = BottomTab.Tasks },
                            modifier = Modifier.weight(1f)
                        )

                        BottomNavTab(
                            label = BottomTab.Settings.label,
                            painter = settingsIcon,
                            selected = selectedTab == BottomTab.Settings,
                            onClick = { selectedTab = BottomTab.Settings },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(TopBarBackground)
                    .clipToBounds()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        // Birday bottom-bar tab transition (nav_enter_anim + nav_exit_anim):
                        // Incoming tab fades in (200ms) + rises 24dp (300ms);
                        // Outgoing tab fades out (200ms) + scales down subtly to 0.85f (300ms).
                        val enterFade = fadeIn(tween(durationMillis = 200, easing = EaseInOut))
                        val enterSlide = slideInVertically(
                            animationSpec = tween<IntOffset>(durationMillis = 300, easing = EaseInOut),
                            initialOffsetY = { tabEnterOffsetPx }
                        )
                        val exitFade = fadeOut(tween(durationMillis = 200, easing = EaseInOut))
                        val exitScale = scaleOut(
                            animationSpec = tween(durationMillis = 300, easing = EaseInOut),
                            targetScale = 0.85f
                        )
                        ContentTransform(
                            targetContentEnter = enterFade + enterSlide,
                            initialContentExit = exitFade + exitScale,
                            targetContentZIndex = 1f
                        )
                    },
                    label = "main_bottom_tab_transition"
                ) { tab ->
                    when (tab) {
                        BottomTab.Tasks -> HomeScreen(
                            viewModel = viewModel,
                            openTaskId = openTaskId,
                            onTaskOpened = onTaskOpened,
                            onProfileClick = {
                                selectedTab = BottomTab.Settings
                                targetSubPage = SettingsSubPage.PROFILE
                            }
                        )
                        BottomTab.Inbox -> InboxScreen(
                            viewModel = viewModel,
                            onAddInboxTaskClick = { showInboxAdd = true }
                        )
                        BottomTab.Settings -> SettingsScreen(
                            onOpenSubPage = { page -> targetSubPage = page },
                            viewModel = viewModel
                        )
                    }
                }

                if (selectedTab != BottomTab.Settings) {
                    // Material You tonal squircle '+' FAB at bottom-right (no border, no glossy sheen)
                    val fabShape = RoundedCornerShape(16.dp)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val fabScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "fab_scale"
                    )
                    val haptic = LocalHapticFeedback.current

                    val isCustomColorfulAccent = remember(Blue500) {
                        val maxC = maxOf(Blue500.red, Blue500.green, Blue500.blue)
                        val minC = minOf(Blue500.red, Blue500.green, Blue500.blue)
                        (maxC - minC) > 0.12f
                    }

                    val fabContainerColor = (if (isCustomColorfulAccent) {
                        lerp(White, Blue500, if (isDark) 0.28f else 0.20f)
                    } else if (isDark) {
                        lerp(Grey100, TextPrimary, 0.06f)
                    } else {
                        lerp(Grey50, Grey100, 0.55f)
                    }).copy(alpha = 1f)

                    val fabContentColor = if (isCustomColorfulAccent) {
                        if (isDark) lerp(Blue500, Color.White, 0.55f)
                        else lerp(Blue500, Color.Black, 0.45f)
                    } else {
                        TextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 18.dp)
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                            .shadow(6.dp, fabShape, clip = false)
                            .size(56.dp)
                            .clip(fabShape)
                            .background(fabContainerColor)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (selectedTab == BottomTab.Tasks) {
                                        showTaskAddDialog = true
                                    } else {
                                        showInboxAdd = true
                                    }
                                },
                                onLongClick = {
                                    if (selectedTab == BottomTab.Tasks) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showTaskAddHighlightedDialog = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = if (selectedTab == BottomTab.Tasks) "Add task" else "Add inbox task",
                            tint = fabContentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                val undoAction by viewModel.undoAction.collectAsStateWithLifecycle()
                UndoFloatingBar(
                    undoAction = undoAction,
                    onUndo = viewModel::undoDelete,
                    onDismiss = viewModel::dismissUndo,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp)
                )
            }
        }

        BirthdayConfettiOverlay(
            triggerKey = birthdayConfettiKey,
            onFinished = { birthdayConfettiKey = 0 },
            modifier = if (isInSettingsSubScreen) Modifier.alpha(0f) else Modifier
        )
        }

        val currentSubPage = displayedSubPage
        if (currentSubPage != null) {
            val touchBlockingModifier = if (targetSubPage != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            } else {
                Modifier
            }
            val onSubPageBack: () -> Unit = { targetSubPage = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = dismissProgress.value.coerceIn(0f, 1f)
                        translationX = containerWidthPx * p
                        shadowElevation = if (p > 0f && p < 1f) subPageShadowPx else 0f
                    }
                    .background(TopBarBackground)
                    .then(touchBlockingModifier)
            ) {
                when (currentSubPage) {
                    SettingsSubPage.PROFILE -> ProfileScreen(onBackClick = onSubPageBack)
                    SettingsSubPage.ABOUT -> AboutAppScreen(onBackClick = onSubPageBack)
                    SettingsSubPage.BACKUP -> BackupScreen(onBackClick = onSubPageBack)
                    SettingsSubPage.THEME -> ThemeScreen(onBackClick = onSubPageBack)
                    SettingsSubPage.LOCK -> LockScreenSettingsScreen(
                        onBackClick = onSubPageBack,
                        onBackupClick = { targetSubPage = SettingsSubPage.BACKUP }
                    )
                    SettingsSubPage.HIDE -> HideTasksSettingsScreen(
                        viewModel = viewModel,
                        onBackClick = onSubPageBack
                    )
                }
            }
        }
    }

    if (showTaskAddDialog) {
        AddTaskDialog(
            isHighlighted = false,
            initialStartDate = selectedDate,
            onDismiss = { showTaskAddDialog = false },
            onAdd = { title, notes, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, _ -> 
                 if (subTasks.isEmpty()) viewModel.addTask(title, notes, false, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 else viewModel.addTaskWithSubTasks(title, notes, false, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 showTaskAddDialog = false
            }
        )
    }
    if (showTaskAddHighlightedDialog) {
        AddTaskDialog(
            isHighlighted = true,
            initialStartDate = selectedDate,
            onDismiss = { showTaskAddHighlightedDialog = false },
            onAdd = { title, notes, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, _ -> 
                 if (subTasks.isEmpty()) viewModel.addTask(title, notes, true, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 else viewModel.addTaskWithSubTasks(title, notes, true, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 showTaskAddHighlightedDialog = false
            }
        )
    }
    if (showInboxAdd) {
        AddTaskDialog(
            isHighlighted = false,
            enableAdvanced = false,
            onDismiss = { showInboxAdd = false },
            onAdd = { title, notes, subTasks, _, _, _, _, _, _, _, _, _, _, _ -> 
                 viewModel.addInboxTask(title, notes, subTasks)
                 showInboxAdd = false
            }
        )
    }
}

@Composable
private fun BottomNavTab(
    label: String,
    painter: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (selected) Blue500 else TextSecondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) Blue500.copy(alpha = 0.16f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tintColor,
            fontFamily = InterText
        )
    }
}
