package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimetableItem
import com.example.data.model.UserProfile
import com.example.ui.StudyViewModel
import com.example.ui.components.AddEditTimetableDialog
import com.example.ui.components.FirestoreSchemaDialog
import com.example.ui.theme.*

@Composable
fun AdminPanelScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val allItems by viewModel.allTimetableItems.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSchemaDialog by remember { mutableStateOf(false) }
    var selectedUserForDetails by remember { mutableStateOf<UserProfile?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Admin Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = VibrantPurple,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Control Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Main Admin: Star • Global study timetables & student metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lock Admin Button
                    FilledTonalButton(
                        onClick = { viewModel.lockAdmin() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_lock_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showSchemaDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = VibrantLavenderContainer,
                            contentColor = VibrantPurpleDark
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schema", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. Realtime Firestore Sync Status & PIN Security Status Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VibrantLavenderContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(VibrantPurple)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Firestore Realtime Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VibrantPurpleDark)
                            Text("Main Admin: Star • PIN: 9044 Active", style = MaterialTheme.typography.bodySmall, color = VibrantLightTextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VibrantPurple
                    ) {
                        Text(
                            text = "LIVE",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 2. Admin Quick Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatCard(
                    title = "Total Sessions",
                    value = allItems.size.toString(),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Administrators",
                    value = allUsers.count { it.role == "admin" }.toString(),
                    icon = Icons.Default.VerifiedUser,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Active Alarms",
                    value = allItems.count { it.alarmEnabled }.toString(),
                    icon = Icons.Default.NotificationsActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Timetable Management Tools
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Timetable Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Create and broadcast schedule updates across all registered students instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_slot_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Slot")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.setSelectedTab(0)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = VibrantPurple)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Calendar", color = VibrantPurple)
                        }
                    }
                }
            }
        }

        // 4. Main Admin & Users Directory
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "User & Admin Directory (${allUsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Account privileges, study targets, and logged progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(allUsers, key = { it.uid }) { student ->
            StudentUserCard(
                student = student,
                onClick = { selectedUserForDetails = student }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add Timetable Slot Dialog
    if (showAddDialog) {
        AddEditTimetableDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { day, subject, topic, start, end, room, notes, colorHex, alarm, ringtoneName, ringtoneType, customRingtoneUri ->
                viewModel.addTimetableItem(day, subject, topic, start, end, room, notes, colorHex, alarm, ringtoneName, ringtoneType, customRingtoneUri)
                showAddDialog = false
            }
        )
    }

    // Schema View Dialog
    if (showSchemaDialog) {
        FirestoreSchemaDialog(onDismiss = { showSchemaDialog = false })
    }

    // Student Details Dialog
    if (selectedUserForDetails != null) {
        val student = selectedUserForDetails!!
        AlertDialog(
            onDismissRequest = { selectedUserForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VibrantPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(student.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📧 Email: ${student.email}", style = MaterialTheme.typography.bodyMedium)
                    Text("🎯 Exam Target: ${student.targetGoal}", style = MaterialTheme.typography.bodyMedium)
                    Text("⏰ Daily Target: ${String.format("%.1f", student.dailyTargetHours)} hrs/day", style = MaterialTheme.typography.bodyMedium)
                    Text("📅 Weekly Target: ${String.format("%.0f", student.weeklyTargetHours)} hrs/week", style = MaterialTheme.typography.bodyMedium)
                    Text("📈 Total Hours Logged: ${String.format("%.1f", student.totalStudyHoursLogged)} hrs", style = MaterialTheme.typography.bodyMedium)
                    Text("🔥 Current Streak: ${student.currentStreakDays} days", style = MaterialTheme.typography.bodyMedium)
                    Text("🛡️ System Role: ${if (student.role == "admin") "MAIN ADMIN" else "STUDENT"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedUserForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = VibrantPillActive,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VibrantPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun StudentUserCard(
    student: UserProfile,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.uid}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = if (student.role == "admin") VibrantPurple else VibrantLavenderContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (student.role == "admin") {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = student.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = VibrantPurpleDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        if (student.role == "admin") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VibrantPurple
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("MAIN ADMIN", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }
                        }
                    }

                    Text(
                        text = student.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibrantPurple,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Text(
                        text = student.targetGoal,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target: ${String.format("%.1f", student.dailyTargetHours)}h/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔥 ${student.currentStreakDays}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
