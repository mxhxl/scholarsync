package com.scholarsync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.scholarsync.api.TokenManager
import com.scholarsync.data.SessionManager
import com.scholarsync.navigation.NavGraph
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.notifications.AlertCheckWorker
import com.scholarsync.notifications.NotificationHelper
import com.scholarsync.ui.components.BottomNavBar
import com.scholarsync.ui.theme.ScholarSyncTheme
import java.util.concurrent.TimeUnit

private val bottomNavRoutes = setOf(
    NavRoutes.Home.route,
    NavRoutes.Discover.route,
    NavRoutes.Library.route,
    NavRoutes.Settings.route
)

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleAlertCheckWorker()
            // TODO: Remove after testing — fires a test notification after permission is granted
            NotificationHelper.showNewPaperNotification(
                context = this,
                notificationId = 12345,
                paperTitle = "Attention Is All You Need: Revisiting Transformer Architectures for Low-Resource NLP",
                topic = "NLP"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize TokenManager with session-expired handler
        TokenManager.init(this) {
            Handler(Looper.getMainLooper()).post {
                SessionManager(this).clearSession()
                sessionExpiredFlag = true
            }
        }

        // Set up notification channel and request permission
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionAndSchedule()

        setContent {
            ScholarSyncTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    var sessionExpired by remember { mutableStateOf(false) }

                    // Check for session expiration flag periodically
                    LaunchedEffect(sessionExpiredFlag) {
                        if (sessionExpiredFlag) {
                            sessionExpiredFlag = false
                            sessionExpired = true
                        }
                    }

                    // Navigate to Welcome when session expires
                    LaunchedEffect(sessionExpired) {
                        if (sessionExpired) {
                            sessionExpired = false
                            navController.navigate(NavRoutes.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    Scaffold(
                        bottomBar = {
                            if (currentRoute in bottomNavRoutes) {
                                BottomNavBar(
                                    currentRoute = currentRoute ?: NavRoutes.Home.route,
                                    onNavigate = { route ->
                                        if (route != currentRoute) {
                                            navController.navigate(route) {
                                                popUpTo(NavRoutes.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { padding ->
                        NavGraph(
                            navController = navController,
                            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleAlertCheckWorker()
                    // TODO: Remove after testing
                    NotificationHelper.showNewPaperNotification(
                        context = this,
                        notificationId = 12345,
                        paperTitle = "Attention Is All You Need: Revisiting Transformer Architectures for Low-Resource NLP",
                        topic = "NLP"
                    )
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Pre-Android 13: permission is granted at install time
            scheduleAlertCheckWorker()
            // TODO: Remove after testing
            NotificationHelper.showNewPaperNotification(
                context = this,
                notificationId = 12345,
                paperTitle = "Attention Is All You Need: Revisiting Transformer Architectures for Low-Resource NLP",
                topic = "NLP"
            )
        }
    }

    private fun scheduleAlertCheckWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AlertCheckWorker>(
            30, TimeUnit.MINUTES // Check every 30 minutes
        )
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES) // Small initial delay
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AlertCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        @Volatile
        var sessionExpiredFlag = false
    }
}
