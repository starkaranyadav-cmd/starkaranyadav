package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmSoundManager
import com.example.ui.StudyViewModel
import com.example.ui.StudyViewModelFactory
import com.example.ui.components.AdminPinDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressTrackerScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.theme.MyApplicationTheme
import android.content.Intent

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels {
        val app = application as StudyApp
        StudyViewModelFactory(app.repository, app.alarmScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Stop any ringing alarm if the app is opened via notification
        if (intent.hasExtra("EXTRA_HIGHLIGHT_SESSION_ID")) {
            AlarmSoundManager.stopAll()
        }

        setContent {
            MyApplicationTheme {
                StudyAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Stop any ringing alarm if the app is re-opened via notification
        if (intent.hasExtra("EXTRA_HIGHLIGHT_SESSION_ID")) {
            AlarmSoundManager.stopAll()
        }
    }
}

@Composable
fun StudyAppRoot(viewModel: StudyViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Runtime Permission for Notifications (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Display banner messages as Snackbars
    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearBanner()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Timetable"
                        )
                    },
                    label = { Text("Timetable") },
                    modifier = Modifier.testTag("nav_tab_timetable")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.TrendingUp else Icons.Outlined.TrendingUp,
                            contentDescription = "Progress"
                        )
                    },
                    label = { Text("Progress") },
                    modifier = Modifier.testTag("nav_tab_progress")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (!isAdminUnlocked) {
                                    Badge {
                                        Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                contentDescription = "Admin"
                            )
                        }
                    },
                    label = { Text("Admin Panel") },
                    modifier = Modifier.testTag("nav_tab_admin")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_tab_profile")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.Chat else Icons.Outlined.Chat,
                            contentDescription = "AI Assistant"
                        )
                    },
                    label = { Text("AI Assistant") },
                    modifier = Modifier.testTag("nav_tab_ai")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "ScreenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> TimetableScreen(viewModel = viewModel)
                1 -> ProgressTrackerScreen(viewModel = viewModel)
                2 -> AdminPanelScreen(viewModel = viewModel)
                3 -> ProfileScreen(viewModel = viewModel)
                4 -> AiAssistantScreen()
            }
        }
    }

    // Admin Security PIN Dialog (PIN 9044)
    if (showPinDialog) {
        AdminPinDialog(
            onDismiss = { viewModel.dismissPinDialog() },
            onVerifyPin = { enteredPin ->
                viewModel.verifyAndUnlockAdmin(enteredPin)
            }
        )
    }
}
