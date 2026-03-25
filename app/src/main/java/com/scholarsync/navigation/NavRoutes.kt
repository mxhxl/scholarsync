package com.scholarsync.navigation

sealed class NavRoutes(val route: String) {
    // Onboarding
    object Welcome : NavRoutes("welcome")
    object Login : NavRoutes("login")
    object ProfileSetupStep1 : NavRoutes("profile_setup_1")
    object ProfileSetupStep2 : NavRoutes("profile_setup_2")
    object ProfileSetupStep3 : NavRoutes("profile_setup_3")

    // Main App
    object Home : NavRoutes("home")
    object Discover : NavRoutes("discover")
    object Library : NavRoutes("library")
    object Settings : NavRoutes("settings")
    object Alerts : NavRoutes("alerts")
    object PaperDetails : NavRoutes("paper_details/{paperId}") {
        fun createRoute(paperId: String) = "paper_details/$paperId"
    }

    // Settings sub-screens
    object EditProfile : NavRoutes("edit_profile")
    object ServerUrl : NavRoutes("server_url")
    object SecurityPrivacy : NavRoutes("security_privacy")
    object ChangePassword : NavRoutes("change_password")
    object LoginActivity : NavRoutes("login_activity")
    object PrivacyPolicy : NavRoutes("privacy_policy")
    object TermsOfService : NavRoutes("terms_of_service")
    object HelpCenter : NavRoutes("help_center")

    // Help Center sub-screens
    object HelpGettingStarted : NavRoutes("help_getting_started")
    object HelpResearchTools : NavRoutes("help_research_tools")
    object HelpAccount : NavRoutes("help_account")
    object HelpTroubleshooting : NavRoutes("help_troubleshooting")
    object HelpFaq : NavRoutes("help_faq")
    object RaiseTicket : NavRoutes("raise_ticket")
    object EmailSupport : NavRoutes("email_support")

    // Library sub-screen
    object Bookmarks : NavRoutes("bookmarks")

    // Streaks
    object Streaks : NavRoutes("streaks")

    // PDF Viewer
    object PdfViewer : NavRoutes("pdf_viewer/{paperId}?pdfUrl={pdfUrl}&paperTitle={paperTitle}") {
        fun createRoute(paperId: String, pdfUrl: String, paperTitle: String): String {
            val encodedUrl = java.net.URLEncoder.encode(pdfUrl, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(paperTitle, "UTF-8")
            return "pdf_viewer/$paperId?pdfUrl=$encodedUrl&paperTitle=$encodedTitle"
        }
    }
}
