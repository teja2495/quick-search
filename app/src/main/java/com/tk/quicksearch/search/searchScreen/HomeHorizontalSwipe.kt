package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.staticCompositionLocalOf

/** Horizontal home-screen gestures that can be forwarded by child swipe surfaces. */
internal enum class HomeHorizontalSwipe {
    RIGHT,
    LEFT,
}

/** Lets child content hand an unavailable horizontal swipe back to the home screen. */
internal val LocalHomeHorizontalSwipeHandler =
    staticCompositionLocalOf<(HomeHorizontalSwipe) -> Unit> { {} }
