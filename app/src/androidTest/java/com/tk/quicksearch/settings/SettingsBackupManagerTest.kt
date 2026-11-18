package com.tk.quicksearch.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportItem
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportOptions
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBackupManagerTest {
    private lateinit var context: Context
    private lateinit var backupFile: File
    private lateinit var originalUserPreferences: Map<String, *>
    private lateinit var originalAppCachePreferences: Map<String, *>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        backupFile = File(context.cacheDir, "settings-backup-test.json")
        originalUserPreferences =
            context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE).all.toMap()
        originalAppCachePreferences =
            context.getSharedPreferences("app_cache", Context.MODE_PRIVATE).all.toMap()
        clearTestPreferences()
    }

    @After
    fun tearDown() {
        clearTestPreferences()
        restorePreferences(BasePreferences.PREFS_NAME, originalUserPreferences)
        restorePreferences("app_cache", originalAppCachePreferences)
        backupFile.delete()
    }

    @Test
    fun settingsRoundTripPreservesEverySupportedPreferenceType() {
        val preferences = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putString("test_backup_string", "value")
            .putBoolean("test_backup_boolean", true)
            .putInt("test_backup_int", 7)
            .putLong("test_backup_long", 8L)
            .putFloat("test_backup_float", 1.25f)
            .putStringSet("test_backup_string_set", setOf("one", "two"))
            .commit()

        SettingsBackupManager.exportToUri(
            context,
            Uri.fromFile(backupFile),
            ExportOptions(setOf(ExportItem.SETTINGS)),
        )
        preferences.edit()
            .putString("test_backup_string", "changed")
            .putBoolean("test_backup_boolean", false)
            .putInt("test_backup_int", -1)
            .putLong("test_backup_long", -1L)
            .putFloat("test_backup_float", -1f)
            .putStringSet("test_backup_string_set", emptySet())
            .commit()

        SettingsBackupManager.importFromUri(context, Uri.fromFile(backupFile))

        assertEquals("value", preferences.getString("test_backup_string", null))
        assertTrue(preferences.getBoolean("test_backup_boolean", false))
        assertEquals(7, preferences.getInt("test_backup_int", 0))
        assertEquals(8L, preferences.getLong("test_backup_long", 0L))
        assertEquals(1.25f, preferences.getFloat("test_backup_float", 0f))
        assertEquals(setOf("one", "two"), preferences.getStringSet("test_backup_string_set", null))
    }

    @Test
    fun selectiveSearchEngineImportDoesNotOverwriteUnrelatedSettings() {
        val preferences = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putBoolean(BasePreferences.KEY_ONE_HANDED_MODE, true)
            .putStringSet(BasePreferences.KEY_DISABLED_SEARCH_ENGINES, setOf("GOOGLE"))
            .commit()
        SettingsBackupManager.exportToUri(
            context,
            Uri.fromFile(backupFile),
            ExportOptions(setOf(ExportItem.SEARCH_ENGINES)),
        )
        preferences.edit()
            .putBoolean(BasePreferences.KEY_ONE_HANDED_MODE, false)
            .putStringSet(BasePreferences.KEY_DISABLED_SEARCH_ENGINES, setOf("BING"))
            .commit()

        SettingsBackupManager.importFromUri(context, Uri.fromFile(backupFile))

        assertFalse(preferences.getBoolean(BasePreferences.KEY_ONE_HANDED_MODE, true))
        assertEquals(
            setOf("GOOGLE"),
            preferences.getStringSet(BasePreferences.KEY_DISABLED_SEARCH_ENGINES, emptySet()),
        )
    }

    @Test
    fun exportOmitsPrivateRuntimeStateAndExcludedPreferenceFiles() {
        val preferences = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putBoolean(BasePreferences.KEY_ONE_HANDED_MODE, true)
            .putBoolean(BasePreferences.KEY_FIRST_LAUNCH, false)
            .putBoolean("feature_flag_test", true)
            .commit()
        context.getSharedPreferences("app_cache", Context.MODE_PRIVATE).edit()
            .putBoolean("catalog_invalidated", true)
            .commit()

        SettingsBackupManager.exportToUri(
            context,
            Uri.fromFile(backupFile),
            ExportOptions(setOf(ExportItem.SETTINGS)),
        )

        val preferencePayload = JSONObject(backupFile.readText()).getJSONObject("preferences")
        val userPayload = preferencePayload.getJSONObject(BasePreferences.PREFS_NAME)
        assertTrue(userPayload.has(BasePreferences.KEY_ONE_HANDED_MODE))
        assertFalse(userPayload.has(BasePreferences.KEY_FIRST_LAUNCH))
        assertFalse(userPayload.has("feature_flag_test"))
        assertFalse(preferencePayload.has("app_cache"))
    }

    private fun clearTestPreferences() {
        context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("test_backup_string")
            .remove("test_backup_boolean")
            .remove("test_backup_int")
            .remove("test_backup_long")
            .remove("test_backup_float")
            .remove("test_backup_string_set")
            .remove(BasePreferences.KEY_ONE_HANDED_MODE)
            .remove(BasePreferences.KEY_DISABLED_SEARCH_ENGINES)
            .remove(BasePreferences.KEY_FIRST_LAUNCH)
            .remove("feature_flag_test")
            .commit()
        context.getSharedPreferences("app_cache", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun restorePreferences(
        name: String,
        values: Map<String, *>,
    ) {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }
}
