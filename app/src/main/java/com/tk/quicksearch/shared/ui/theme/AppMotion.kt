package com.tk.quicksearch.shared.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion specs for the whole app. Components take their springs from here, never from inline
 * `spring(...)` values, so a new motion language is a change to this file alone.
 *
 * The spatial springs are the Material 3 Expressive motion scheme. material3 1.4 keeps
 * `MotionScheme.expressive()` internal, so the values are copied here; once it is public, read
 * them from `MaterialTheme.motionScheme` instead and pass that scheme to the theme.
 */
object AppMotion {
    /** Damping of the fast spatial spring: small components (button shape morphs). */
    const val FastSpatialDamping = 0.6f

    /** Stiffness of the fast spatial spring. */
    const val FastSpatialStiffness = 800f

    /** Damping of the default spatial spring: partial-screen movement and rotation. */
    const val DefaultSpatialDamping = 0.8f

    /** Stiffness of the default spatial spring. */
    const val DefaultSpatialStiffness = 380f

    /** Fast spatial spring for small shape changes, such as a pressed button squaring its corners. */
    fun <T> fastSpatial(): SpringSpec<T> =
        spring(dampingRatio = FastSpatialDamping, stiffness = FastSpatialStiffness)

    /** Default spatial spring for moves and rotations, such as a section chevron turning. */
    fun <T> defaultSpatial(): SpringSpec<T> =
        spring(dampingRatio = DefaultSpatialDamping, stiffness = DefaultSpatialStiffness)

    /** Critically damped settle, e.g. a released page swipe or pager tab sliding home. */
    fun <T> settle(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = visibilityThreshold,
        )

    /** Slow, bouncy morph for looping loading shapes. */
    fun <T> loadingMorph(): SpringSpec<T> = spring(dampingRatio = FastSpatialDamping, stiffness = Spring.StiffnessLow)

    /** Scale and fade of a grid tile being dragged, merged into a folder, or dropped. */
    fun <T> dragFeedback(): SpringSpec<T> = spring(stiffness = Spring.StiffnessMediumLow)

    // Durations (ms) for tweens and fades -------------------------------------------------

    /** Micro-hold between animation phases. */
    const val DurationMicro = 50

    /** Short transitions such as height changes. */
    const val DurationShort = 200

    /** Medium transitions such as entry/exit fades and the wallpaper fade-in. */
    const val DurationMedium = 300

    /** Glow and border cool-down fades. */
    const val DurationGlowFade = 500

    /** Long welcome and promo effects. */
    const val DurationLong = 4000
}
