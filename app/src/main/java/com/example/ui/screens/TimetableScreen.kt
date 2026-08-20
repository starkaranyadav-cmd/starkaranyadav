package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimetableItem
import com.example.ui.StudyViewModel
import com.example.ui.components.AddEditTimetableDialog
import com.example.ui.components.QuickLogSessionDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val itemsForDay by viewModel.dayTimetableItems.collectAsState()
    val allItems by viewModel.allTimetableItems.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TimetableItem?>(null) }
    var itemToDelete by remember { mutableStateOf<TimetableItem?>(null) }
    var quickLogItem by remember { mutableStateOf<TimetableItem?>(null) }

    val todayDay = remember { StudyViewModel.getTodayDayOfWeek() }
    val isAdmin = currentRole == "admin"
    val profile = userProfile ?: com.example.data.model.UserProfile()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header (Vibrant Palette style: Avatar + Name + Role + Actions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = VibrantLavenderContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    color = VibrantPurpleDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAdmin) VibrantPurple else VibrantPillActive
                                ) {
                                    Text(
                                        text = if (isAdmin) "ADMIN" else "STUDENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) Color.White else VibrantPurpleDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Goal: ${profile.targetGoal}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Role Switcher Button
                    FilledTonalButton(
                        onClick = { viewModel.toggleRole() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = VibrantPillActive,
                            contentColor = VibrantPurpleDark
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("role_switch_button")
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.School else Icons.Default.AdminPanelSettings,
                            contentDescription = "Switch Role",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAdmin) "To Student" else "To Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Day Selector Chips (Vibrant Palette Pills)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TimetableItem.DAYS_OF_WEEK) { day ->
                        val isSelected = selectedDay.equals(day, ignoreCase = true)
                        val isToday = todayDay.equals(day, ignoreCase = true)
                        val dayItemCount = allItems.count { it.dayOfWeek.equals(day, ignoreCase = true) }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) VibrantLavenderContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clickable { viewModel.setSelectedDay(day) }
                                .testTag("day_chip_$day")
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, VibrantPurple, RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(VibrantPurple)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = day.take(3),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) VibrantPurpleDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) VibrantPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = dayItemCount.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) VibrantPurpleDark else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VibrantPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_session_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Study Slot")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Day Header & Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$selectedDay's Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val totalMinutes = itemsForDay.sumOf { it.durationMinutes }
                        val totalHours = String.format("%.1f", totalMinutes / 60f)
                        Text(
                            text = "${itemsForDay.size} session(s) • Total $totalHours study hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isAdmin) {
                        FilledTonalButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = VibrantLavenderContainer,
                                contentColor = VibrantPurpleDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Slot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Next Up / Active Banner (if viewing today)
            if (selectedDay.equals(todayDay, ignoreCase = true) && itemsForDay.isNotEmpty()) {
                item {
                    val nextSession = itemsForDay.firstOrNull()
                    if (nextSession != null) {
                        ActiveSessionHeroCard(
                            session = nextSession,
                            onStartStudying = {
                                viewModel.startStopwatch(nextSession.subject, nextSession.topic)
                                viewModel.setSelectedTab(1) // switch to Progress/Timer tab
                            }
                        )
                    }
                }
            }

            // Timetable Sessions List (Vibrant Palette 4.dp border & card style)
            if (itemsForDay.isEmpty()) {
                item {
                    EmptyDayState(
                        day = selectedDay,
                        isAdmin = isAdmin,
                        onAddClicked = { showAddDialog = true }
                    )
                }
            } else {
                items(itemsForDay, key = { it.id }) { item ->
                    TimetableCard(
                        item = item,
                        isAdmin = isAdmin,
                        onToggleAlarm = { enabled -> viewModel.toggleAlarm(item, enabled) },
                        onEdit = { itemToEdit = item },
                        onDelete = { itemToDelete = item },
                        onQuickLog = { quickLogItem = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Session Dialog
    if (showAddDialog) {
        AddEditTimetableDialog(
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onConfirm = { day, subject, topic, start, end, room, notes, colorHex, alarm, ringtoneName, ringtoneType, customRingtoneUri ->
                viewModel.addTimetableItem(day, subject, topic, start, end, room, notes, colorHex, alarm, ringtoneName, ringtoneType, customRingtoneUri)
                showAddDialog = false
            }
        )
    }

    // Edit Session Dialog
    if (itemToEdit != null) {
        AddEditTimetableDialog(
            itemToEdit = itemToEdit,
            defaultDay = itemToEdit!!.dayOfWeek,
            onDismiss = { itemToEdit = null },
            onConfirm = { day, subject, topic, start, end, room, notes, colorHex, alarm, ringtoneName, ringtoneType, customRingtoneUri ->
                val updated = itemToEdit!!.copy(
                    dayOfWeek = day,
                    subject = subject,
                    topic = topic,
                    startTime = start,
                    endTime = end,
                    roomOrLocation = room,
                    notes = notes,
                    colorHex = colorHex,
                    alarmEnabled = alarm,
                    ringtoneName = ringtoneName,
                    ringtoneType = ringtoneType,
                    customRingtoneUri = customRingtoneUri,
                    durationMinutes = calculateDuration(start, end),
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updateTimetableItem(updated)
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Timetable Slot?") },
            text = { Text("Are you sure you want to remove '${itemToDelete?.subject}' from $selectedDay's timetable?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteTimetableItem(it) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Log Session Dialog
    if (quickLogItem != null) {
        QuickLogSessionDialog(
            initialSubject = quickLogItem!!.subject,
            initialTopic = quickLogItem!!.topic,
            onDismiss = { quickLogItem = null },
            onConfirm = { subject, topic, duration, notes ->
                viewModel.quickLogSession(subject, topic, duration, notes)
                quickLogItem = null
            }
        )
    }
}

@Composable
fun ActiveSessionHeroCard(
    session: TimetableItem,
    onStartStudying: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = VibrantLavenderContainer,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VibrantPillActive
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VibrantPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEXT SESSION TODAY",
                            color = VibrantPurpleDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = session.getFormattedTimeRange(),
                    color = VibrantPurpleDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = session.subject,
                color = VibrantPurpleDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (session.topic.isNotBlank()) {
                Text(
                    text = session.topic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = VibrantPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.roomOrLocation,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onStartStudying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("start_studying_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Timer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TimetableCard(
    item: TimetableItem,
    isAdmin: Boolean,
    onToggleAlarm: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickLog: () -> Unit
) {
    val accentColor = if (item.alarmEnabled) parseHexColor(item.colorHex) else VibrantLightOutline
    val parts = remember(item.startTime) {
        val p = item.startTime.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: 9
        val m = p.getOrNull(1) ?: "00"
        val amPm = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        Pair(String.format("%02d:%s", h12, m), amPm)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.alarmEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.alarmEnabled) 1.5.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_item_${item.id}")
            .then(
                if (!item.alarmEnabled) Modifier.border(1.dp, VibrantLightOutlineVariant, RoundedCornerShape(18.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left 4dp Accent Border from Vibrant Palette design HTML
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time box with vertical divider
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(58.dp)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = parts.first,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = parts.second,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Vertical Divider Line (#CAC4D0)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(VibrantLightOutline)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Subject Title & Topic Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtext = if (item.topic.isNotBlank()) item.topic else item.roomOrLocation
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.notes.isNotBlank()) {
                        Text(
                            text = "💡 ${item.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Action / Status Indicator Pills from Vibrant Palette Design HTML
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (item.alarmEnabled) VibrantPurple else VibrantPillActive,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onToggleAlarm(!item.alarmEnabled) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Alarm Toggle",
                                tint = if (item.alarmEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDayState(
    day: String,
    isAdmin: Boolean,
    onAddClicked: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.EventBusy,
                contentDescription = null,
                tint = VibrantPurple,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No study sessions on $day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enjoy your break or add study sessions to stay ahead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule $day Session")
            }
        }
    }
}

private fun calculateDuration(start: String, end: String): Int {
    return try {
        val sParts = start.split(":").map { it.toInt() }
        val eParts = end.split(":").map { it.toInt() }
        val sMin = sParts[0] * 60 + sParts[1]
        val eMin = eParts[0] * 60 + eParts[1]
        if (eMin >= sMin) eMin - sMin else (24 * 60 - sMin) + eMin
    } catch (e: Exception) {
        60
    }
}
