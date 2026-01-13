package com.tk.quicksearch.services

import android.app.Application
import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.interfaces.PermissionService
import com.tk.quicksearch.permissions.PermissionRequestHandler

/**
 * Implementation of PermissionService for handling permission operations
 */
class PermissionServiceImpl(
    private val application: Application,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences
) : PermissionService {

    override fun hasContactPermission(): Boolean {
        return contactRepository.hasPermission()
    }

    override fun hasFilePermission(): Boolean {
        return fileRepository.hasPermission()
    }

    override fun hasCallPermission(): Boolean {
        return PermissionRequestHandler.checkCallPermission(application)
    }

    override fun refreshOptionalPermissions(): Boolean {
        // For now, just check current permission state
        // TODO: Implement refresh logic if needed
        return hasContactPermission() || hasFilePermission()
    }

    override fun refreshUsageAccess() {
        // This is handled by the AppUsageRepository, but we can expose it here if needed
        // For now, just delegate to the existing implementation
    }
}