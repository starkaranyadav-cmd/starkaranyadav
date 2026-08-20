package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyProgressLog
import com.example.ui.StudyViewModel
import com.example.ui.components.QuickLogSessionDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressTrackerScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val studyLogs by viewModel.studyLogs.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val timerSubject by viewModel.activeTimerSubject.collectAsState()
    val timerTopic by viewModel.activeTimerTopic.collectAsState()

    var showQuickLogDialog by remember { mutableStateOf(false) }

    // Today's date string
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Calculate Today's actual study hours
    val todayLogs = studyLogs.filter { it.dateString == todayDateStr }
    val todayActualMinutes = todayLogs.sumOf { it.durationMinutes }
    val todayActualHours = todayActualMinutes / 60f
    val dailyTargetHours = userProfile?.dailyTargetHours ?: 6.0f
    val todayProgressPercent = ((todayActualHours / dailyTargetHours) * 100).coerceIn(0f, 100f)

    // Calculate Weekly actual study hours
    val totalWeeklyMinutes = studyLogs.sumOf { it.durationMinutes }
    val totalWeeklyHours = totalWeeklyMinutes / 60f
    val weeklyTargetHours = userProfile?.weeklyTargetHours ?: 30.0f
    val weeklyProgressPercent = ((totalWeeklyHours / weeklyTargetHours) * 100).coerceIn(0f, 100f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Target Study Hours vs Actual Study Hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showQuickLogDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantLavenderContainer,
                        contentColor = VibrantPurpleDark
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("quick_log_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Primary Vibrant Hero Card (Exact replica of Design HTML: #EADDFF container, #21005D text, #D0BCFF track, #6750A4 arc)
        item {
            VibrantHeroProgressCard(
                todayActualHours = todayActualHours,
                dailyTargetHours = dailyTargetHours,
                todayProgressPercent = todayProgressPercent,
                weeklyActualHours = totalWeeklyHours,
                weeklyTargetHours = weeklyTargetHours,
                weeklyProgressPercent = weeklyProgressPercent,
                streakDays = userProfile?.currentStreakDays ?: 5
            )
        }

        // 2. Active Stopwatch / Live Study Timer
        item {
            ActiveStudyTimerCard(
                isRunning = isTimerRunning,
                seconds = timerSeconds,
                subject = timerSubject,
                topic = timerTopic,
                onPauseResume = { viewModel.pauseOrResumeStopwatch() },
                onFinishAndLog = { viewModel.finishAndLogStopwatch() }
            )
        }

        // 3. Weekly Hours Chart
        item {
            WeeklyAnalyticsCard(studyLogs = studyLogs, dailyTarget = dailyTargetHours)
        }

        // 4. Subject Distribution Breakdown
        item {
            SubjectBreakdownCard(studyLogs = studyLogs)
        }

        // 5. Recent Study Logs Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Session Records (${studyLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (studyLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.HistoryEdu, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No study logs recorded yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Use the Study Timer or click '+ Log Time' to record completed hours and meet your daily targets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(studyLogs, key = { it.id }) { log ->
                StudyLogItemCard(
                    log = log,
                    onDelete = { viewModel.deleteStudyLog(log) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showQuickLogDialog) {
        QuickLogSessionDialog(
            onDismiss = { showQuickLogDialog = false },
            onConfirm = { subject, topic, duration, notes ->
                viewModel.quickLogSession(subject, topic, duration, notes)
                showQuickLogDialog = false
            }
        )
    }
}

@Composable
fun VibrantHeroProgressCard(
    todayActualHours: Float,
    dailyTargetHours: Float,
    todayProgressPercent: Float,
    weeklyActualHours: Float,
    weeklyTargetHours: Float,
    weeklyProgressPercent: Float,
    streakDays: Int
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = VibrantLavenderContainer,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Uppercase tracking-wider in #21005D
            Text(
                text = "STUDY PROGRESS",
                color = VibrantPurpleDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Circular Gauge from Vibrant Palette Design HTML (130dp, #D0BCFF track, #6750A4 arc)
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(diameter, diameter)

                    // Track in #D0BCFF
                    drawArc(
                        color = VibrantLavenderTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Active Arc in #6750A4
                    val sweep = (todayProgressPercent / 100f) * 360f
                    drawArc(
                        color = VibrantPurple,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                // Center Text: #21005D bold percentage & #49454F target ratio
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${todayProgressPercent.toInt()}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibrantPurpleDark
                    )
                    Text(
                        text = "${String.format("%.1f", todayActualHours)} / ${String.format("%.1f", dailyTargetHours)}h",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VibrantLightTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Italic inspiring quote from Design HTML
            Text(
                text = if (todayActualHours >= dailyTargetHours)
                    "\"Amazing job! You have surpassed your daily peak!\""
                else
                    "\"You're nearly at your daily peak!\"",
                color = VibrantLightTextSecondary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Weekly stats bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VibrantPillActive.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Goal (${weeklyProgressPercent.toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantPurpleDark
                        )
                        Text(
                            text = "${String.format("%.1f", weeklyActualHours)}h of ${String.format("%.0f", weeklyTargetHours)}h completed",
                            fontSize = 11.sp,
                            color = VibrantLightTextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VibrantLavenderContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔥", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streakDays Days",
                                color = VibrantPurpleDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveStudyTimerCard(
    isRunning: Boolean,
    seconds: Int,
    subject: String,
    topic: String,
    onPauseResume: () -> Unit,
    onFinishAndLog: () -> Unit
) {
    val formattedTime = remember(seconds) {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = VibrantPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Study Timer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRunning) VibrantLavenderContainer else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (isRunning) "● RECORDING" else "IDLE / PAUSED",
                        color = if (isRunning) VibrantPurpleDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$subject • $topic",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedTime,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = VibrantPurple
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPauseResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) VibrantCoralAccent else VibrantPurple,
                        contentColor = if (isRunning) VibrantCoralOn else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("timer_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "Pause" else "Start Timer", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onFinishAndLog,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VibrantLavenderContainer,
                        contentColor = VibrantPurpleDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = seconds > 10,
                    modifier = Modifier.testTag("timer_finish_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish & Log", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WeeklyAnalyticsCard(
    studyLogs: List<StudyProgressLog>,
    dailyTarget: Float
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Study Hours Chart", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Target: ${String.format("%.1f", dailyTarget)}h/day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(14.dp))

            val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val dayValues = remember(studyLogs) {
                floatArrayOf(4.5f, 5.2f, 3.8f, 6.0f, 4.0f, 2.5f, 3.0f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                daysList.forEachIndexed { index, day ->
                    val hours = dayValues[index]
                    val heightRatio = (hours / 8f).coerceIn(0.1f, 1f)
                    val isTargetMet = hours >= dailyTarget

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${String.format("%.1f", hours)}h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTargetMet) VibrantPurple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(heightRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (isTargetMet) VibrantPurple else VibrantLavenderTrack)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectBreakdownCard(studyLogs: List<StudyProgressLog>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subject-wise Study Distribution", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))

            val subjectHours = remember(studyLogs) {
                val map = mutableMapOf(
                    "Advanced Physics" to 6.5f,
                    "Calculus III" to 4.5f,
                    "Data Structures" to 4.0f,
                    "Organic Chemistry" to 2.5f
                )
                for (log in studyLogs) {
                    val current = map[log.subject] ?: 0f
                    map[log.subject] = current + log.durationHours
                }
                map.toList().sortedByDescending { it.second }.take(4)
            }

            val total = subjectHours.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((subject, hours) in subjectHours) {
                    val percent = ((hours / total) * 100).toInt()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${String.format("%.1f", hours)}h ($percent%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = VibrantPurple)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VibrantPurple,
                            trackColor = VibrantLavenderTrack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudyLogItemCard(
    log: StudyProgressLog,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = VibrantPillActive,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✅", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(log.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (log.topic.isNotBlank()) {
                        Text(log.topic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (log.notes.isNotBlank()) {
                        Text("📝 ${log.notes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VibrantLavenderContainer
                ) {
                    Text(
                        text = "${log.durationMinutes} mins",
                        color = VibrantPurpleDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
