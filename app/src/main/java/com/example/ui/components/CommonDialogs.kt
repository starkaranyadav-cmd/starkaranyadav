package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.FirestoreSchemaInfo
import com.example.data.model.TimetableItem
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else VibrantLavenderContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isError) Icons.Default.LockReset else Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else VibrantPurpleDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin Security PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Enter the 4-digit PIN to access timetable controls and student management",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4 PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> MaterialTheme.colorScheme.error
                                        isFilled -> VibrantPurple
                                        else -> VibrantLavenderTrack
                                    }
                                )
                                .then(
                                    if (isFilled && !isError) Modifier.border(2.dp, VibrantPurpleDark, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Incorrect PIN! Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Numeric Keypad (1 to 9, Clear, 0, Backspace)
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (row in keypadRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (key in row) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = when (key) {
                                        "C", "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> VibrantLavenderContainer.copy(alpha = 0.6f)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable {
                                            isError = false
                                            when (key) {
                                                "C" -> pin = ""
                                                "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> {
                                                    if (pin.length < 4) {
                                                        val newPin = pin + key
                                                        pin = newPin
                                                        if (newPin.length == 4) {
                                                            val success = onVerifyPin(newPin)
                                                            if (!success) {
                                                                isError = true
                                                                pin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("keypad_btn_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (key) {
                                                "C", "⌫" -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> VibrantPurpleDark
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTimetableDialog(
    itemToEdit: TimetableItem? = null,
    defaultDay: String = "Monday",
    onDismiss: () -> Unit,
    onConfirm: (
        day: String,
        subject: String,
        topic: String,
        start: String,
        end: String,
        room: String,
        notes: String,
        colorHex: String,
        alarmEnabled: Boolean,
        ringtoneName: String,
        ringtoneType: String,
        customRingtoneUri: String?
    ) -> Unit
) {
    var selectedDay by remember { mutableStateOf(itemToEdit?.dayOfWeek ?: defaultDay) }
    var subject by remember { mutableStateOf(itemToEdit?.subject ?: "") }
    var topic by remember { mutableStateOf(itemToEdit?.topic ?: "") }
    var startTime by remember { mutableStateOf(itemToEdit?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(itemToEdit?.endTime ?: "10:30") }
    var room by remember { mutableStateOf(itemToEdit?.roomOrLocation ?: "Desk / Hall A") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }
    var selectedColor by remember { mutableStateOf(itemToEdit?.colorHex ?: TimetableItem.DEFAULT_COLORS[0]) }
    var alarmEnabled by remember { mutableStateOf(itemToEdit?.alarmEnabled ?: true) }
    
    var ringtoneName by remember { mutableStateOf(itemToEdit?.ringtoneName ?: "Morning Chimes") }
    var ringtoneType by remember { mutableStateOf(itemToEdit?.ringtoneType ?: "chimes") }
    var customRingtoneUri by remember { mutableStateOf(itemToEdit?.customRingtoneUri) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                customRingtoneUri = it.toString()
                ringtoneName = "Custom Song"
                ringtoneType = "custom"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var subjectError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (itemToEdit == null) Icons.Default.AddCircle else Icons.Default.Edit,
                    contentDescription = null,
                    tint = VibrantPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (itemToEdit == null) "Add Study Session" else "Edit Study Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day Selector
                Text("Day of Week", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val shortDays = listOf("Mon" to "Monday", "Tue" to "Tuesday", "Wed" to "Wednesday", "Thu" to "Thursday", "Fri" to "Friday", "Sat" to "Saturday", "Sun" to "Sunday")
                    for ((shortName, fullName) in shortDays) {
                        val isSelected = selectedDay.equals(fullName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) VibrantPurple else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDay = fullName }
                        ) {
                            Text(
                                text = shortName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Subject Name
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (it.isNotBlank()) subjectError = false
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Advanced Physics, Calculus III") },
                    isError = subjectError,
                    supportingText = if (subjectError) { { Text("Subject name is required") } } else null,
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = VibrantPurple) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_input_field")
                )

                // Topic Name
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Chapter") },
                    placeholder = { Text("e.g. Quantum Mechanics: Wave Equations") },
                    leadingIcon = { Icon(Icons.Default.Topic, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Times Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time (24h)") },
                        placeholder = { Text("09:00") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time (24h)") },
                        placeholder = { Text("10:30") },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Room / Location
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Location / Study Space") },
                    placeholder = { Text("e.g. Library Quiet Zone, Room 102") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes / Goals") },
                    placeholder = { Text("Bring formula sheet, solve questions 1-10...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Picker
                Text("Color Tag", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (hex in TimetableItem.DEFAULT_COLORS) {
                        val color = parseHexColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, VibrantPurpleDark, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Alarm Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantLavenderContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (alarmEnabled) VibrantPurple else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Loud Reminder Alarm", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Triggers AlarmManager at $startTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = alarmEnabled,
                                onCheckedChange = { alarmEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantPurple
                                )
                            )
                        }
                        
                        if (alarmEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ringtone: $ringtoneName", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) {
                                    Text("Upload Custom Song")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isBlank()) {
                        subjectError = true
                    } else {
                        onConfirm(
                            selectedDay,
                            subject,
                            topic,
                            startTime,
                            endTime,
                            room,
                            notes,
                            selectedColor,
                            alarmEnabled,
                            ringtoneName,
                            ringtoneType,
                            customRingtoneUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                modifier = Modifier.testTag("confirm_session_button")
            ) {
                Text(if (itemToEdit == null) "Add Session" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickLogSessionDialog(
    initialSubject: String = "",
    initialTopic: String = "",
    onDismiss: () -> Unit,
    onConfirm: (subject: String, topic: String, durationMinutes: Int, notes: String) -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject.ifEmpty { "General Study" }) }
    var topic by remember { mutableStateOf(initialTopic) }
    var durationMinutes by remember { mutableStateOf(60) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VibrantPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Completed Study Time", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = VibrantPurple) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic Studied") },
                    leadingIcon = { Icon(Icons.Default.Topic, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Study Duration: ${durationMinutes} minutes (${String.format("%.1f", durationMinutes / 60f)} hours)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = it.toInt() },
                    valueRange = 15f..240f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantPurple,
                        activeTrackColor = VibrantPurple,
                        inactiveTrackColor = VibrantLavenderTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Key Learnings / Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(subject, topic, durationMinutes, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                modifier = Modifier.testTag("confirm_log_button")
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, goal: String, dailyTarget: Float, weeklyTarget: Float, avatarId: Int) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var goal by remember { mutableStateOf(profile.targetGoal) }
    var dailyTarget by remember { mutableStateOf(profile.dailyTargetHours) }
    var weeklyTarget by remember { mutableStateOf(profile.weeklyTargetHours) }
    var avatarId by remember { mutableStateOf(profile.avatarId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = VibrantPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile & Goals", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = VibrantPurple) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Primary Exam / Target Goal") },
                    placeholder = { Text("e.g. Master Calculus & Physics for Finals") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Daily Target: ${String.format("%.1f", dailyTarget)} hours/day",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = dailyTarget,
                    onValueChange = { dailyTarget = (it * 2).toInt() / 2f },
                    valueRange = 1f..12f,
                    steps = 21,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantPurple,
                        activeTrackColor = VibrantPurple,
                        inactiveTrackColor = VibrantLavenderTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Weekly Target: ${String.format("%.0f", weeklyTarget)} hours/week",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = weeklyTarget,
                    onValueChange = { weeklyTarget = (it).toInt().toFloat() },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantPurple,
                        activeTrackColor = VibrantPurple,
                        inactiveTrackColor = VibrantLavenderTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, email, goal, dailyTarget, weeklyTarget, avatarId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FirestoreSchemaDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = VibrantPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Firebase & Architecture", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Architecture Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = VibrantLavenderContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = FirestoreSchemaInfo.ARCHITECTURE_OVERVIEW.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text("Firestore JSON Schema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = FirestoreSchemaInfo.FIRESTORE_SCHEMA_JSON.trim(),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        VibrantPurple
    }
}
