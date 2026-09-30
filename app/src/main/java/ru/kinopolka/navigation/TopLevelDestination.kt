package ru.kinopolka.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.kinopolka.R

/** Tabs of the bottom navigation bar: «Главная», «Поиск», «Моя полка». */
enum class TopLevelDestination(
    val graph: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME(HomeGraph, R.string.tab_home, R.drawable.ic_home),
    SEARCH(SearchGraph, R.string.tab_search, R.drawable.ic_search),
    LIBRARY(LibraryGraph, R.string.tab_library, R.drawable.ic_library),
}
