package com.tk.quicksearch.widgetsPanel

/** Compare only host-owned options; providers may store additional keys in the same bundle. */
internal fun widgetOptionsMatch(current: Map<String, Any?>, desired: Map<String, Any?>): Boolean =
    desired.all { (key, value) -> current.containsKey(key) && current[key] == value }
