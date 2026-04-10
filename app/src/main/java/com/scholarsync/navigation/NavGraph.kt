package com.scholarsync.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.screens.*

private const val ANIM_DURATION = 300

// ── Slide transitions ────────────────────────────────────────────────────────

private val slideInFromRight = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIM_DURATION)
)

private val slideOutToLeft = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIM_DURATION)
)

private val slideInFromLeft = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(ANIM_DURATION)
)

private val slideOutToRight = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(ANIM_DURATION)
)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Welcome.route,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }

    // Auto-navigate to Home if user is already logged in
    val effectiveStart = if (sessionManager.isLoggedIn()) NavRoutes.Home.route else startDestination

    NavHost(
        navController = navController,
        startDestination = effectiveStart,
        modifier = modifier,
        enterTransition = { slideInFromRight },
        exitTransition = { slideOutToLeft },
        popEnterTransition = { slideInFromLeft },
        popExitTransition = { slideOutToRight }
    ) {
        // ── Onboarding Flow ─────────────────────────────────────────────────

        composable(NavRoutes.Welcome.route) {
            var displayName by remember { mutableStateOf(sessionManager.getDisplayName()) }
            LaunchedEffect(Unit) {
                displayName = sessionManager.getDisplayName()
            }
            WelcomeScreen(
                onGetStarted = { navController.navigate(NavRoutes.ProfileSetupStep1.route) },
                onSignIn = { navController.navigate(NavRoutes.Login.route) },
                userDisplayName = displayName,
                onServerUrl = { navController.navigate(NavRoutes.ServerUrl.route) }
            )
        }

        composable(NavRoutes.Login.route) {
            LoginScreen(
                onSignInSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ProfileSetupStep1.route) {
            ProfileSetupStep1Screen(
                onContinue = { name ->
                    sessionManager.setDisplayName(name)
                    navController.navigate(NavRoutes.ProfileSetupStep2.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ProfileSetupStep2.route) {
            ProfileSetupStep2Screen(
                onContinue = { navController.navigate(NavRoutes.ProfileSetupStep3.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ProfileSetupStep3.route) {
            ProfileSetupStep3Screen(
                onFinish = { navController.navigate(NavRoutes.Home.route) {
                    popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                }},
                onSkip = { navController.navigate(NavRoutes.Home.route) {
                    popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                }}
            )
        }

        // ── Main App Screens ─────────────────────────────────────────────────

        composable(NavRoutes.Home.route) {
            HomeScreen(
                onPaperClick = { paperId ->
                    navController.navigate(NavRoutes.PaperDetails.createRoute(paperId))
                },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(NavRoutes.Discover.route) {
            DiscoverScreen(
                onNavigate = { route -> navController.navigate(route) },
                onPaperClick = { paperId ->
                    navController.navigate(NavRoutes.PaperDetails.createRoute(paperId))
                }
            )
        }

        composable(NavRoutes.Library.route) {
            LibraryScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = {
                    sessionManager.clearSession()
                    navController.navigate(NavRoutes.Welcome.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Pushed screens (slide transitions — inherited from NavHost) ─────

        composable(NavRoutes.Alerts.route) {
            AlertsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PaperDetails.route) { backStackEntry ->
            val paperId = backStackEntry.arguments?.getString("paperId") ?: ""
            PaperDetailsScreen(
                paperId = paperId,
                onBack = { navController.popBackStack() },
                onNavigateToAlerts = {
                    navController.navigate(NavRoutes.Alerts.route) {
                        launchSingleTop = true
                    }
                },
                onOpenPdf = { pdfUrl, title ->
                    navController.navigate(NavRoutes.PdfViewer.createRoute(paperId, pdfUrl, title))
                }
            )
        }

        composable(
            route = NavRoutes.PdfViewer.route,
            arguments = listOf(
                navArgument("paperId") { type = NavType.StringType },
                navArgument("pdfUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("paperTitle") { type = NavType.StringType; defaultValue = "Paper" }
            )
        ) { backStackEntry ->
            val pdfUrl = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("pdfUrl") ?: "", "UTF-8"
            )
            val paperTitle = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("paperTitle") ?: "Paper", "UTF-8"
            )
            PdfViewerScreen(
                pdfUrl = pdfUrl,
                paperTitle = paperTitle,
                onBack = { navController.popBackStack() }
            )
        }

        // Settings sub-screens
        composable(NavRoutes.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ServerUrl.route) {
            ServerUrlScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.SecurityPrivacy.route) {
            SecurityPrivacyScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(NavRoutes.ChangePassword.route) },
                onLoginActivity = { navController.navigate(NavRoutes.LoginActivity.route) },
                onPrivacyPolicy = { navController.navigate(NavRoutes.PrivacyPolicy.route) },
                onTermsOfService = { navController.navigate(NavRoutes.TermsOfService.route) }
            )
        }
        composable(NavRoutes.ChangePassword.route) {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.LoginActivity.route) {
            LoginActivityScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.TermsOfService.route) {
            TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.HelpCenter.route) {
            HelpCenterScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGettingStarted = { navController.navigate(NavRoutes.HelpGettingStarted.route) },
                onNavigateToResearchTools = { navController.navigate(NavRoutes.HelpResearchTools.route) },
                onNavigateToAccount = { navController.navigate(NavRoutes.HelpAccount.route) },
                onNavigateToTroubleshooting = { navController.navigate(NavRoutes.HelpTroubleshooting.route) },
                onNavigateToFaq = { navController.navigate(NavRoutes.HelpFaq.route) },
                onNavigateToRaiseTicket = { navController.navigate(NavRoutes.RaiseTicket.route) },
                onNavigateToEmailSupport = { navController.navigate(NavRoutes.EmailSupport.route) }
            )
        }
        composable(NavRoutes.HelpGettingStarted.route) {
            HelpGettingStartedScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.HelpResearchTools.route) {
            HelpResearchToolsScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.HelpAccount.route) {
            HelpAccountScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.HelpTroubleshooting.route) {
            HelpTroubleshootingScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.HelpFaq.route) {
            HelpFaqScreen(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.RaiseTicket.route) {
            RaiseTicketScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.EmailSupport.route) {
            EmailSupportScreen(onBack = { navController.popBackStack() })
        }

        // Streaks
        composable(NavRoutes.Streaks.route) {
            StreaksScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Library sub-screen
        composable(NavRoutes.Bookmarks.route) {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onPaperClick = { paperId ->
                    navController.navigate(NavRoutes.PaperDetails.createRoute(paperId))
                }
            )
        }
    }
}
