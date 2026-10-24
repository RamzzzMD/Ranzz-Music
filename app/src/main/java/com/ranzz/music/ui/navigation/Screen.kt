package com.ranzz.music.ui.navigation

import androidx.annotation.DrawableRes
import com.ranzz.music.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_nav_home)
    object Search : Screen("search", "Search", R.drawable.ic_nav_search)
    object Library : Screen("library", "Library", R.drawable.ic_nav_library)
    object Favorite : Screen("favorite", "Favorite", R.drawable.ic_nav_favorite)

    companion object {
        val bottomNavItems = listOf(Home, Search, Library)
    }
}
