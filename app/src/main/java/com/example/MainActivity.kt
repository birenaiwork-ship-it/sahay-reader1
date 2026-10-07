package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.audiobook.AudiobookPlayerScreen
import com.example.ui.audiobook.AudiobookScreen
import com.example.ui.bookmarks.BookmarksScreen
import com.example.ui.home.HomeScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.navigation.Screen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SahayaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SahayaApplication
        val prefsRepo = app.preferencesRepository

        setContent {
            val preferences by prefsRepo.preferencesFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            SahayaTheme(
                isHighContrast = preferences?.highContrast ?: false,
                isLargeText = preferences?.largeText ?: false
            ) {
                preferences?.let { prefs ->
                    SahayaAppNavHost(
                        hasCompletedOnboarding = prefs.hasCompletedOnboarding,
                        onCompleteOnboarding = {
                            scope.launch { prefsRepo.setOnboardingCompleted(true) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SahayaAppNavHost(
    hasCompletedOnboarding: Boolean,
    onCompleteOnboarding: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (hasCompletedOnboarding) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinue = {
                    onCompleteOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSkip = {
                    onCompleteOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Home
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReader = { bookId, page, unitId ->
                    navController.navigate(Screen.Reader.createRoute(bookId, page, unitId))
                },
                onNavigateToLibrary = {
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToAudiobooks = {
                    navController.navigate(Screen.Audiobooks.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Library
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenBook = { bookId, page, unitId ->
                    navController.navigate(Screen.Reader.createRoute(bookId, page, unitId))
                },
                onGenerateAudiobook = { bookId ->
                    navController.navigate("audiobooks?preselectedBookId=$bookId")
                },
                onViewBookmarks = { bookId ->
                    navController.navigate(Screen.Bookmarks.createRoute(bookId))
                }
            )
        }

        // Reader
        composable(
            route = "reader/{bookId}?page={page}&unitId={unitId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("unitId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val page = backStackEntry.arguments?.getInt("page") ?: 1
            val unitId = backStackEntry.arguments?.getString("unitId")

            ReaderScreen(
                bookId = bookId,
                initialPage = page,
                initialUnitId = unitId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearch = { bId ->
                    navController.navigate(Screen.Search.createRoute(bId))
                },
                onNavigateToAudiobook = { bId ->
                    navController.navigate("audiobooks?preselectedBookId=$bId")
                }
            )
        }

        // Bookmarks
        composable(
            route = Screen.Bookmarks.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookmarksScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
                onJumpToBookmark = { page, unitId ->
                    navController.navigate(Screen.Reader.createRoute(bookId, page, unitId)) {
                        popUpTo(Screen.Reader.route) { inclusive = true }
                    }
                }
            )
        }

        // Search
        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            SearchScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
                onSelectResult = { page, unitId ->
                    navController.navigate(Screen.Reader.createRoute(bookId, page, unitId)) {
                        popUpTo(Screen.Reader.route) { inclusive = true }
                    }
                }
            )
        }

        // Audiobooks
        composable(
            route = "audiobooks?preselectedBookId={preselectedBookId}",
            arguments = listOf(
                navArgument("preselectedBookId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val preselectedBookId = backStackEntry.arguments?.getString("preselectedBookId")
            AudiobookScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayAudiobook = { audiobookId ->
                    navController.navigate(Screen.AudiobookPlayer.createRoute(audiobookId))
                },
                preselectedBookId = preselectedBookId
            )
        }

        composable(Screen.Audiobooks.route) {
            AudiobookScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlayAudiobook = { audiobookId ->
                    navController.navigate(Screen.AudiobookPlayer.createRoute(audiobookId))
                }
            )
        }

        // Audiobook Player
        composable(
            route = Screen.AudiobookPlayer.route,
            arguments = listOf(navArgument("audiobookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val audiobookId = backStackEntry.arguments?.getString("audiobookId") ?: ""
            AudiobookPlayerScreen(
                audiobookId = audiobookId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
