package com.ranzz.music.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ranzz.music.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, @DrawableRes val iconRes: Int) {
    object Home : Screen("home", R.string.tab_home, R.drawable.ic_nav_home)
    object Search : Screen("search", R.string.tab_search, R.drawable.ic_nav_search)
    object Library : Screen("library", R.string.tab_library, R.drawable.ic_nav_library)
    object Favorite : Screen("favorite", R.string.lib_tab_favorites, R.drawable.ic_nav_favorite)

    companion object {
        val bottomNavItems = listOf(Home, Search, Library)
    }
}
