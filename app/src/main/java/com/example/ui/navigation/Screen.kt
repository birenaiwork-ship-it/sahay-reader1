package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Reader : Screen("reader/{bookId}?page={page}&unitId={unitId}") {
        fun createRoute(bookId: String, page: Int = 1, unitId: String? = null): String {
            return if (unitId != null) {
                "reader/$bookId?page=$page&unitId=$unitId"
            } else {
                "reader/$bookId?page=$page"
            }
        }
    }
    data object Bookmarks : Screen("bookmarks/{bookId}") {
        fun createRoute(bookId: String): String = "bookmarks/$bookId"
    }
    data object AllBookmarks : Screen("all_bookmarks")
    data object Audiobooks : Screen("audiobooks")
    data object AudiobookPlayer : Screen("audiobook_player/{audiobookId}") {
        fun createRoute(audiobookId: String): String = "audiobook_player/$audiobookId"
    }
    data object Search : Screen("search/{bookId}") {
        fun createRoute(bookId: String): String = "search/$bookId"
    }
    data object Settings : Screen("settings")
}
