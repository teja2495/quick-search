package com.tk.quicksearch.shared.permissions

import com.tk.quicksearch.BuildConfig

/** The prominent disclosure is required for the Play-distributed build only. */
val shouldShowAccessibilityDisclosure: Boolean
    get() = BuildConfig.FLAVOR != "fdroid"
