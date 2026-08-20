package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.StudyViewModel
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.FirestoreSchemaDialog
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSchemaDialog by remember { mutableStateOf(false) }

    val profile = userProfile ?: UserProfile()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Profile Header
            Text(
                text = "User Profile & Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage personal study targets, alarms, and credentials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. User Profile Card (Vibrant Lavender Container header)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = VibrantLavenderContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VibrantPurpleDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentRole == "admin") VibrantPurple else VibrantPillActive
                                ) {
                                    Text(
                                        text = if (currentRole == "admin") "ADMIN PRIVILEGES" else "STUDENT ROLE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (currentRole == "admin") Color.White else VibrantPurpleDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = VibrantPurple)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Goal Display
                    Text("Target Exam / Academic Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.targetGoal, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Hours Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VibrantLavenderContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Daily Target", style = MaterialTheme.typography.labelSmall, color = VibrantLightTextSecondary)
                                Text("${String.format("%.1f", profile.dailyTargetHours)} hrs/day", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VibrantPurpleDark)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VibrantLavenderContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Weekly Target", style = MaterialTheme.typography.labelSmall, color = VibrantLightTextSecondary)
                                Text("${String.format("%.0f", profile.weeklyTargetHours)} hrs/wk", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VibrantPurpleDark)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VibrantLavenderContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Streak", style = MaterialTheme.typography.labelSmall, color = VibrantLightTextSecondary)
                                Text("🔥 ${profile.currentStreakDays} days", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VibrantPurpleDark)
                            }
                        }
                    }
                }
            }
        }

        // 2. Alarms & Notifications Core Section (AlarmManager & BroadcastReceiver)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alarms & Exact Reminders", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "Powered by Android AlarmManager & BroadcastReceiver with exact wakeup alarms and high-priority push notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Permission & Channel Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VibrantLavenderContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("AlarmManager Service Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VibrantPurpleDark)
                                Text("Exact alarms configured • Notification channel enabled", style = MaterialTheme.typography.labelSmall, color = VibrantLightTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Alarm Trigger Button
                    Button(
                        onClick = { viewModel.triggerTestAlarm() },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_alarm_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Loud Alarm (Trigger in 3s)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Schema & System Architecture
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backend Architecture & Schema", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "Inspect the Firebase Realtime Database & Firestore JSON schema, Room DB mapping, and AlarmManager pipeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    OutlinedButton(
                        onClick = { showSchemaDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = VibrantPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Firestore JSON Schema", color = VibrantPurple)
                    }
                }
            }
        }

        // 4. Role Switcher Quick Action
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = VibrantLavenderContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Switch Active Role", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = VibrantPurpleDark)
                        Text("Current role: ${currentRole.uppercase()}", style = MaterialTheme.typography.bodySmall, color = VibrantLightTextSecondary)
                    }

                    Button(
                        onClick = { viewModel.toggleRole() },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentRole == "admin") "To Student" else "To Admin")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, email, goal, dailyTarget, weeklyTarget, avatarId ->
                viewModel.updateProfile(name, email, goal, dailyTarget, weeklyTarget, avatarId)
                showEditProfileDialog = false
            }
        )
    }

    if (showSchemaDialog) {
        FirestoreSchemaDialog(onDismiss = { showSchemaDialog = false })
    }
}
