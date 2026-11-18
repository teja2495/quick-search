package com.tk.quicksearch.shared.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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

    /** No-bounce glide of a grid tile into its new slot after a reorder or drop. */
    fun <T> placement(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(stiffness = Spring.StiffnessMedium, visibilityThreshold = visibilityThreshold)

    // Easing for tweens --------------------------------------------------------------------
    // Material 3 easing curves. Use these instead of Material 2's FastOutSlowIn, LinearOutSlowIn
    // and FastOutLinearIn.

    /** Emphasized easing for most transitions: a move, resize, or crossfade that starts and ends on screen. */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized decelerate for elements entering the screen. */
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized accelerate for elements leaving the screen. */
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Durations (ms) for tweens and fades -------------------------------------------------
    // Steps of the Material 3 duration scale (short1 50 ... long1 450, extraLong1 700). Snap a
    // fade or tween to the nearest step rather than adding a one-off value.

    /** Micro-hold between animation phases (Material short1). */
    const val DurationMicro = 50

    /** Tiny fades, such as a thumbnail or overlay badge appearing (Material short2). */
    const val DurationQuick = 100

    /** Exits and small icon swaps, which run faster than the matching enter (Material short3). */
    const val DurationExit = 150

    /** Short transitions such as height changes and enter fades (Material short4). */
    const val DurationShort = 200

    /** Enters that also move or scale, such as chrome sliding in (Material medium1). */
    const val DurationStandard = 250

    /** Medium transitions such as entry/exit fades and the wallpaper fade-in (Material medium2). */
    const val DurationMedium = 300

    /** Whole-surface enters, such as the overlay opening or the background crossfade (Material medium4). */
    const val DurationLarge = 400

    /** Slow ambient crossfades, such as the rotating search hint (Material extraLong1). */
    const val DurationExtraLong = 700

    /** Glow and border cool-down fades. */
    const val DurationGlowFade = 500

    /** Long welcome and promo effects. */
    const val DurationLong = 4000
}
