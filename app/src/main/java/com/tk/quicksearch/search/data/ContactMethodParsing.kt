package com.tk.quicksearch.search.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.util.PackageConstants.WHATSAPP_BUSINESS_PACKAGE
import java.util.Collections
import java.util.Locale

/** Maps one ContactsContract Data row to a [ContactMethod], or null when the row is unsupported or fails to parse. */
    internal fun ContactRepository.parseContactMethod(
        mimeType: String,
        data1: String,
        data2: String?,
        data3: String?,
        data4: String?,
        data5: String?,
        resPackage: String?,
        dataId: Long,
        isPrimary: Boolean,
    ): ContactMethod? =
        try {
            when (mimeType) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    ContactMethod.Phone(callLabel, data1, dataId, isPrimary)
                }

                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    ContactMethod.Email(emailLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_VOICE_CALL -> {
                    ContactMethod.WhatsAppCall(whatsAppVoiceCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_MESSAGE -> {
                    ContactMethod.WhatsAppMessage(whatsAppMessageLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL -> {
                    ContactMethod.WhatsAppVideoCall(whatsAppVideoCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL,
                -> {
                    parseWhatsAppBusinessMethod(
                        mimeType = mimeType,
                        data = data1,
                        dataId = dataId,
                        isPrimary = isPrimary,
                    )
                }

                ContactMethodMimeTypes.TELEGRAM_MESSAGE -> {
                    ContactMethod.TelegramMessage(telegramMessageLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.TELEGRAM_CALL -> {
                    ContactMethod.TelegramCall(telegramVoiceCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL -> {
                    ContactMethod.TelegramVideoCall(telegramVideoCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.SIGNAL_MESSAGE -> {
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == ContactRepository.SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                ContactMethodMimeTypes.SIGNAL_CALL -> {
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == ContactRepository.SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                ContactMethodMimeTypes.SIGNAL_VIDEO_CALL -> {
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == ContactRepository.SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                else -> {
                    if (mimeType.startsWith("vnd.android.cursor.item/vnd.org.thoughtcrime.securesms")) {
                        val packageName =
                            resolveSignalLikePackage(
                                mimeType = mimeType,
                                data3 = data3,
                                data4 = data4,
                                data5 = data5,
                                resPackage = resPackage,
                            )

                        if (packageName != null && (packageName != ContactRepository.SIGNAL_PACKAGE || isSignalPackageBrandedAsMolly)) {
                            parseCustomAppMethod(
                                mimeType = mimeType,
                                data = data1,
                                dataId = dataId,
                                isPrimary = isPrimary,
                                packageNameOverride = packageName,
                                displayLabelOverride = data3,
                            )
                        } else {
                            when {
                                mimeType.contains("video", ignoreCase = true) ->
                                    ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                                mimeType.contains("call", ignoreCase = true) ->
                                    ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                                else ->
                                    ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                            }
                        }
                    } else if (mimeType.startsWith(ContactRepository.VND_MIME_PREFIX)) {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            displayLabelOverride = data3,
                        )
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(ContactRepository.TAG, "Error parsing contact method: $mimeType", e)
            null
        }

    /**
     * Attempts to extract package name from custom MIME type.
     * Format: vnd.android.cursor.item/vnd.com.package.name.xxx
     */
    internal fun ContactRepository.extractPackageFromMimeType(mimeType: String): String? {
        if (!mimeType.startsWith(ContactRepository.VND_MIME_PREFIX)) return null

        val cached = packageNameByMimeCache[mimeType]
        if (cached != null || packageNameByMimeCache.containsKey(mimeType)) {
            return cached
        }

        val resolved =
            run {
                val rest = mimeType.substring(ContactRepository.VND_MIME_PREFIX.length)
                val parts = rest.split(ContactRepository.PACKAGE_SEPARATOR)
                if (parts.size < ContactRepository.PACKAGE_PARTS_MIN_COUNT) {
                    return@run null
                }

                // Prefer the longest prefix that maps to an installed package.
                for (partCount in parts.size - 1 downTo ContactRepository.PACKAGE_PARTS_MIN_COUNT) {
                    val candidate = parts.take(partCount).joinToString(ContactRepository.PACKAGE_SEPARATOR)
                    if (isPackageInstalled(candidate)) {
                        return@run candidate
                    }
                }

                // Fallback to first two segments when installation cannot be resolved.
                parts.take(ContactRepository.PACKAGE_PARTS_MIN_COUNT).joinToString(ContactRepository.PACKAGE_SEPARATOR)
            }

        packageNameByMimeCache[mimeType] = resolved
        return resolved
    }

    internal fun ContactRepository.resolveCustomAppDisplayLabel(packageName: String?): String {
        if (packageName.isNullOrBlank()) return otherLabel

        val cached = customAppLabelCache[packageName]
        if (cached != null) return cached

        val resolved =
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo)?.toString()?.takeIf { it.isNotBlank() }
            }.getOrNull() ?: packageName

        customAppLabelCache[packageName] = resolved
        return resolved
    }

    internal fun ContactRepository.parseCustomAppMethod(
        mimeType: String,
        data: String,
        dataId: Long,
        isPrimary: Boolean,
        packageNameOverride: String? = null,
        displayLabelOverride: String? = null,
    ): ContactMethod.CustomApp {
        val packageName = packageNameOverride ?: extractPackageFromMimeType(mimeType)
        val displayLabel =
            displayLabelOverride?.takeIf { it.isNotBlank() } ?: resolveCustomAppDisplayLabel(packageName)
        return ContactMethod.CustomApp(
            displayLabel = displayLabel,
            data = data,
            mimeType = mimeType,
            packageName = packageName,
            dataId = dataId,
            isPrimary = isPrimary,
        )
    }

    internal fun ContactRepository.parseWhatsAppBusinessMethod(
        mimeType: String,
        data: String,
        dataId: Long,
        isPrimary: Boolean,
    ): ContactMethod.CustomApp {
        val actionLabel =
            when (mimeType) {
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL ->
                    context.getString(R.string.contacts_action_button_voice_call)
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL ->
                    context.getString(R.string.contacts_action_button_video_call)
                else -> context.getString(R.string.contacts_action_button_chat)
            }
        return ContactMethod.CustomApp(
            displayLabel = "${resolveCustomAppDisplayLabel(WHATSAPP_BUSINESS_PACKAGE)} $actionLabel",
            data = data,
            mimeType = mimeType,
            packageName = WHATSAPP_BUSINESS_PACKAGE,
            dataId = dataId,
            isPrimary = isPrimary,
        )
    }

    internal fun ContactRepository.resolveSignalLikePackage(
        mimeType: String,
        data3: String?,
        data4: String?,
        data5: String?,
        resPackage: String?,
        includeMimeTypeHints: Boolean = true,
    ): String? {
        val fromResPackage = resPackage?.takeIf { it.isNotBlank() }
        if (fromResPackage != null) return fromResPackage

        val fields =
            if (includeMimeTypeHints) {
                listOf(data5, data4, data3, mimeType)
            } else {
                listOf(data5, data4, data3)
            }
        val packageCandidates =
            fields
                .flatMap { field -> extractPackageCandidatesFromField(field) }
                .map { candidate -> normalizeSignalLikePackageCandidate(candidate) }
                .filterNotNull()
                .distinct()

        val installedCandidate = packageCandidates.firstOrNull(::isPackageInstalled)
        if (installedCandidate != null) return installedCandidate
        if (packageCandidates.isNotEmpty()) return packageCandidates.first()

        val hasMollyHint = fields.any { it?.contains("molly", ignoreCase = true) == true }
        if (hasMollyHint) {
            return resolveInstalledMollyPackageName() ?: ContactRepository.MOLLY_PACKAGE_CANDIDATES.first()
        }

        // Some Molly contact methods reuse Signal MIME types without explicit package metadata.
        // If Signal is unavailable but Molly is installed, classify it as Molly custom app.
        if (!isPackageInstalled(ContactRepository.SIGNAL_PACKAGE)) {
            resolveInstalledMollyPackageName()?.let { return it }
        }

        // Some Molly builds reuse the Signal package name but expose Molly branding.
        if (isSignalPackageBrandedAsMolly) {
            return ContactRepository.SIGNAL_PACKAGE
        }

        return null
    }

    internal fun ContactRepository.normalizeSignalLikePackageCandidate(candidate: String): String? {
        val normalized = candidate.trim()
        if (normalized.isBlank()) return null

        if (ContactRepository.MOLLY_PACKAGE_CANDIDATES.any { normalized.contains(it) }) {
            return ContactRepository.MOLLY_PACKAGE_CANDIDATES.first { normalized.contains(it) }
        }
        if (normalized.contains(ContactRepository.SIGNAL_PACKAGE)) {
            return ContactRepository.SIGNAL_PACKAGE
        }

        // Ignore MIME namespace-like tokens (for example "vnd.android.cursor.item").
        if (normalized.startsWith("vnd.")) {
            return null
        }

        return normalized
    }

    internal fun ContactRepository.resolveInstalledMollyPackageName(): String? =
        ContactRepository.MOLLY_PACKAGE_CANDIDATES.firstOrNull(::isPackageInstalled) ?: discoveredMollyPackageName

    internal fun ContactRepository.extractPackageCandidatesFromField(field: String?): List<String> {
        if (field.isNullOrBlank()) return emptyList()
        return ContactRepository.PACKAGE_NAME_PATTERN.findAll(field).map { it.value }.toList()
    }

    internal fun ContactRepository.isPackageInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
