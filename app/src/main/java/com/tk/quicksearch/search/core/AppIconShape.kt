package com.tk.quicksearch.search.core

/** Controls the clipping shape applied to app icons throughout the UI. */
enum class AppIconShape {
    /** Icons are shown with their natural shape (square / adaptive icon, legacy icons rounded). */
    SQUARE,

    /** Icons are clipped to a circle. */
    CIRCLE,
}