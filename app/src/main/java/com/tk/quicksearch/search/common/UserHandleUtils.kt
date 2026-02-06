package com.tk.quicksearch.search.common

import android.os.Build
import android.os.UserHandle

object UserHandleUtils {
    fun getIdentifier(userHandle: UserHandle): Int =
        runCatching {
            val method = UserHandle::class.java.getMethod("getIdentifier")
            method.invoke(userHandle) as Int
        }.getOrDefault(0)

    fun of(identifier: Int): UserHandle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { UserHandle::class.java.getMethod("of", Int::class.javaPrimitiveType).invoke(null, identifier) as UserHandle }.getOrNull()
        } else {
            null
        }
}
