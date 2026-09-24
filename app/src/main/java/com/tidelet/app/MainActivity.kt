package com.tidelet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tidelet.app.ui.home.HomeScreen
import com.tidelet.app.ui.journal.JournalDetailScreen
import com.tidelet.app.ui.journal.EveningReviewsListScreen
import com.tidelet.app.ui.journal.JournalHubScreen
import com.tidelet.app.ui.journal.JournalListScreen
import com.tidelet.app.ui.journal.ThoughtCheckDetailScreen
import com.tidelet.app.ui.journal.ThoughtChecksListScreen
import com.tidelet.app.ui.checkin.EveningReviewScreen
import com.tidelet.app.ui.log.CheckInScreen
import com.tidelet.app.ui.log.LogScreen
import com.tidelet.app.ui.nav.Routes
import com.tidelet.app.ui.onboarding.OnboardingFlow
import com.tidelet.app.ui.settings.SettingsScreen
import com.tidelet.app.ui.cbt.DistortionsLibraryScreen
import com.tidelet.app.ui.cbt.RefusalRehearsalScreen
import com.tidelet.app.ui.cbt.RelapsePlanScreen
import com.tidelet.app.ui.sos.BreatheScreen
import com.tidelet.app.ui.sos.CompassionateResetScreen
import com.tidelet.app.ui.sos.DistractionsScreen
import com.tidelet.app.ui.sos.JournalHubScreen
import com.tidelet.app.ui.sos.JournalScreen
import com.tidelet.app.ui.sos.ReasonsScreen
import com.tidelet.app.ui.sos.RideTheWaveScreen
import com.tidelet.app.ui.sos.SosScreen
import com.tidelet.app.ui.sos.ThoughtCheckScreen
import com.tidelet.app.ui.stats.StatsScreen
import com.tidelet.app.ui.theme.TideletTheme
import com.tidelet.app.ui.weekly.WeeklyReflectionScreen

/**
 * The app's single Activity.
 *
 * One-activity architecture: we never swap Activities. All navigation happens inside
 * Compose's [NavHost]. The Activity just hosts Compose, applies the theme, and
 * decides whether to show onboarding or the main app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the platform splash before super.onCreate so the
        // compat library can attach its window before the Activity's view
        // tree is created. The splash background + animated icon come from
        // Theme.Tidelet.Splash (see res/values/themes.xml).
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = (application as TideletApplication).repository

        // Keep the splash visible while we read the profile from DataStore so
        // the user never sees a brief blank frame between splash and Home.
        // The condition flips to false the moment `profile` is non-null in the
        // composition below, which lets the OS dismiss the splash normally.
        var profileLoaded = false
        splash.setKeepOnScreenCondition { !profileLoaded }

        setContent {
            TideletTheme {
                val profile by repo.profile.collectAsStateWithLifecycle(initialValue = null)

                // Flip the splash gate as soon as DataStore has answered.
                if (profile != null) profileLoaded = true

                when {
                    // First frame: we haven't read prefs yet. Render nothing (or a splash)
                    // to avoid a flash of the wrong screen.
                    profile == null -> Unit

                    // Not onboarded yet: show the onboarding flow, nothing else.
                    profile?.onboardingComplete == false -> OnboardingFlow()

                    // Onboarded: the full app.
                    else -> TideletApp()
                }
            }
        }
    }
}

/**
 * Top-level composable for the onboarded app.
 * Holds the bottom nav + the NavHost that swaps screens beneath it.
 */
@Composable
fun TideletApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val showBottomBar = currentRoute in setOf(
        Routes.HOME, Routes.LOG, Routes.JOURNAL, Routes.STATS, Routes.SETTINGS,
        // Keep the bottom bar visible on the check-in form — it's part of the
        // Log tab's flow, not a modal takeover like SOS.
        Routes.LOG_CHECKIN,
        // Ditto for the weekly reflection — launched from Home and feels
        // like a peer to the check-in form.
        Routes.WEEKLY_REFLECTION,
        // The Journal read-side list + detail screens are children of the
        // Journal tab and feel like "I'm still on Journal" — keep the tab
        // bar visible so the user can nav away without going Back repeatedly.
        Routes.JOURNAL_THOUGHT_CHECKS,
        Routes.JOURNAL_THOUGHT_CHECK_DETAIL,
        Routes.JOURNAL_ENTRIES,
        Routes.JOURNAL_ENTRY_DETAIL,
        Routes.JOURNAL_EVENING_REVIEWS,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TideletBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Bottom-nav destinations
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSos = { navController.navigate(Routes.SOS) },
                    onOpenWeeklyReflection = { navController.navigate(Routes.WEEKLY_REFLECTION) },
                    onWriteLetter = { days ->
                        navController.navigate(Routes.milestoneLetter(days))
                    },
                    onOpenThoughtCheck = { id ->
                        navController.navigate(Routes.journalThoughtCheckDetail(id))
                    },
                    onOpenJournalEntry = { id ->
                        navController.navigate(Routes.journalEntryDetail(id))
                    },
                    onOpenEveningReview = { navController.navigate(Routes.EVENING_REVIEW) },
                )
            }
            composable(Routes.LOG) {
                LogScreen(
                    onOpenCheckIn = { date ->
                        navController.navigate(Routes.logCheckIn(date.toString()))
                    },
                )
            }
            composable(
                route = Routes.LOG_CHECKIN,
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) {
                CheckInScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.EVENING_REVIEW) {
                EveningReviewScreen(onDone = { navController.popBackStack() })
            }
            // --- Journal (read-side) bottom-nav tab + its children ---
            composable(Routes.JOURNAL) {
                JournalHubScreen(
                    onOpenThoughtChecks = { navController.navigate(Routes.JOURNAL_THOUGHT_CHECKS) },
                    onOpenJournalEntries = { navController.navigate(Routes.JOURNAL_ENTRIES) },
                    onOpenEveningReviews = { navController.navigate(Routes.JOURNAL_EVENING_REVIEWS) },
                    onWriteThoughtCheck = { navController.navigate(Routes.SOS_THOUGHT_CHECK) },
                    onWriteFreely = { navController.navigate(Routes.SOS_JOURNAL_FREE) },
                )
            }
            composable(Routes.JOURNAL_EVENING_REVIEWS) {
                EveningReviewsListScreen()
            }
            composable(Routes.JOURNAL_THOUGHT_CHECKS) {
                ThoughtChecksListScreen(
                    onOpenDetail = { id ->
                        navController.navigate(Routes.journalThoughtCheckDetail(id))
                    },
                )
            }
            composable(
                route = Routes.JOURNAL_THOUGHT_CHECK_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                ThoughtCheckDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.JOURNAL_ENTRIES) {
                JournalListScreen(
                    onOpenDetail = { id ->
                        navController.navigate(Routes.journalEntryDetail(id))
                    },
                )
            }
            composable(
                route = Routes.JOURNAL_ENTRY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                JournalDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenRelapsePlan = { navController.navigate(Routes.RELAPSE_PLAN) },
                    onOpenRefusalRehearsal = { navController.navigate(Routes.REFUSAL_REHEARSAL) },
                    onOpenDistortionsLibrary = { navController.navigate(Routes.CBT_DISTORTIONS) },
                    onOpenResources = { navController.navigate(Routes.RESOURCES) },
                    onOpenGettingStarted = { navController.navigate(Routes.GETTING_STARTED) },
                )
            }

            composable(Routes.WEEKLY_REFLECTION) {
                WeeklyReflectionScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Routes.MILESTONE_LETTER,
                arguments = listOf(navArgument("days") { type = NavType.StringType }),
            ) {
                com.tidelet.app.ui.milestones.MilestoneLetterScreen(
                    onDone = { navController.popBackStack() },
                )
            }

            // SOS flow (modal-feeling — no bottom bar while inside)
            composable(Routes.SOS) {
                SosScreen(
                    onClose = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(
                route = "${Routes.SOS_WAVE}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                RideTheWaveScreen(
                    onDone = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onDrankFlow = {
                        navController.navigate(Routes.SOS_RESET) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    intensity = intensity,
                )
            }
            composable(Routes.SOS_RESET) {
                CompassionateResetScreen(
                    onLogToday = {
                        val today = java.time.LocalDate.now().toString()
                        navController.navigate(Routes.logCheckIn(today)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onLater = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
            composable(
                route = "${Routes.SOS_BREATHE}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                BreatheScreen(onDone = { navController.popBackStack() }, intensity = intensity)
            }
            composable(
                route = "${Routes.SOS_REASONS}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                ReasonsScreen(onBack = { navController.popBackStack() }, intensity = intensity)
            }
            composable(
                route = "${Routes.SOS_DISTRACTIONS}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                DistractionsScreen(onDone = { navController.popBackStack() }, intensity = intensity)
            }
            composable(
                route = "${Routes.SOS_JOURNAL}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                val suffix = intensity?.let { "?intensity=$it" } ?: ""
                JournalHubScreen(
                    onOpenFreeform = { navController.navigate("${Routes.SOS_JOURNAL_FREE}$suffix") },
                    onOpenThoughtCheck = { navController.navigate("${Routes.SOS_THOUGHT_CHECK}$suffix") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "${Routes.SOS_JOURNAL_FREE}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                JournalScreen(
                    onDone = { navController.popBackStack() },
                    onViewEntries = {
                        navController.popBackStack()
                        navController.navigate(Routes.JOURNAL_ENTRIES)
                    },
                    intensity = intensity,
                )
            }
            composable(
                route = "${Routes.SOS_THOUGHT_CHECK}?intensity={intensity}",
                arguments = listOf(navArgument("intensity") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val intensity = entry.arguments?.getInt("intensity")?.takeIf { it > 0 }
                ThoughtCheckScreen(
                    onDone = { navController.popBackStack() },
                    onOpenDistortions = { navController.navigate(Routes.CBT_DISTORTIONS) },
                    intensity = intensity,
                )
            }

            // --- CBT surfaces (reachable from Settings + in-flow jumps) ---
            composable(Routes.CBT_DISTORTIONS) {
                DistortionsLibraryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCheckIn = {
                        // Bottom "habit" callout → jump into today's check-in form.
                        // Matches the LogScreen / CompassionateResetScreen pattern.
                        navController.navigate(
                            Routes.logCheckIn(java.time.LocalDate.now().toString())
                        )
                    },
                )
            }
            composable(Routes.RELAPSE_PLAN) {
                RelapsePlanScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.REFUSAL_REHEARSAL) {
                RefusalRehearsalScreen(onBack = { navController.popBackStack() })
            }

            // --- Help & resources ---
            composable(Routes.RESOURCES) {
                com.tidelet.app.ui.resources.ResourcesScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.GETTING_STARTED) {
                com.tidelet.app.ui.help.GettingStartedScreen(
                    onBack = { navController.popBackStack() },
                    onOpenResources = { navController.navigate(Routes.RESOURCES) },
                )
            }
        }
    }
}

@Composable
private fun TideletBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

// ---- Bottom-nav items ----

private enum class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Home(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    Log(Routes.LOG, R.string.nav_log, Icons.Rounded.EditCalendar),
    Journal(Routes.JOURNAL, R.string.nav_journal, Icons.Rounded.AutoStories),
    Stats(Routes.STATS, R.string.nav_stats, Icons.Rounded.Insights),
    Settings(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
}
