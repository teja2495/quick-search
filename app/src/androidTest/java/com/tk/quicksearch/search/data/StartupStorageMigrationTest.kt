package com.tk.quicksearch.search.data

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tk.quicksearch.search.core.SearchViewModelInitialStateFactory
import com.tk.quicksearch.search.data.assets.ManagedAssetStore
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.BootstrapPreferences
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupStorageMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun bootstrapFirstLaunchReadsTinyDedicatedState() {
        context.getSharedPreferences("bootstrap_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(BasePreferences.FIRST_LAUNCH_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(BasePreferences.KEY_FIRST_LAUNCH, false)
            .commit()

        assertFalse(BootstrapPreferences.isFirstLaunch(context))
    }

    @Test
    fun startupPreferencesIgnoreStaleDuplicatedSnapshot() {
        val central = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        central.edit()
            .putBoolean("one_handed_mode", true)
            .putFloat("wallpaper_blur_radius", 18f)
            .putFloat("wallpaper_background_alpha", 0.35f)
            .commit()
        context.getSharedPreferences("startup_preferences_snapshot", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putInt("snapshot_version", 1)
            .putBoolean("one_handed_mode", false)
            .putFloat("wallpaper_blur_radius", 0f)
            .putFloat("wallpaper_background_alpha", 1f)
            .commit()

        val startup = UserAppPreferences(context).getStartupPreferences()

        assertTrue(startup.oneHandedMode)
        assertEquals(18f, startup.wallpaperBlurRadius)
        assertEquals(0.35f, startup.wallpaperBackgroundAlpha)
    }

    @Test
    fun initialSearchStateRestoresSelectedIconPack() {
        val preferences = UserAppPreferences(context)
        val selectedPackage = "com.example.iconpack"
        preferences.setSelectedIconPackPackage(selectedPackage)

        try {
            val initialState =
                SearchViewModelInitialStateFactory.create(
                    appContext = context,
                    startupPreferencesReader = preferences,
                    startupSurfaceStore = StartupSurfaceStore(context),
                    inMemoryRetainedQuery = "",
                )

            assertEquals(selectedPackage, initialState.configState.selectedIconPackPackage)
        } finally {
            preferences.setSelectedIconPackPackage(null)
        }
    }

    @Test
    fun triggerMigrationPartitionsOverlappingPrefixes() {
        context.getSharedPreferences("search_customization_index", Context.MODE_PRIVATE).edit().clear().commit()
        val legacy = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        legacy.edit()
            .putString(
                "${BasePreferences.KEY_TRIGGER_APP_PREFIX}pkg",
                JSONObject().put("word", "app").put("afterSpace", false).toString(),
            )
            .putString(
                "${BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX}shortcut",
                JSONObject().put("word", "shortcut").put("afterSpace", true).toString(),
            )
            .commit()

        val words = TriggerPreferences(context).getAllTriggerWordsById()

        assertEquals("app", words["app:pkg"])
        assertEquals("shortcut", words["shortcut:shortcut"])
        assertNull(words["app:shortcut_shortcut"])
    }

    @Test
    fun roomStoreSupportsBatchedIdsAndAtomicReplacement() {
        val notes =
            listOf(
                NoteInfo(91L, "One", "first", 1L, 2L),
                NoteInfo(92L, "Two", "second", 3L, 4L),
            )
        val store = NotesRoomStore(context)
        store.replaceFromBackup(notes)

        assertEquals(listOf(92L), store.getByIds(listOf(92L)).map { it.noteId })
        assertEquals(notes.map { it.noteId }.toSet(), store.getAll().map { it.noteId }.toSet())
    }

    @Test
    fun managedAssetsEnforceLimitAndRoundTrip() {
        val store = ManagedAssetStore(context)
        val encoded = Base64.encodeToString(byteArrayOf(1, 2, 3, 4), Base64.NO_WRAP)
        assertTrue(store.putBase64("test:small", encoded))
        assertEquals(encoded, store.getBase64("test:small"))

        val oversized = ByteArray(ManagedAssetStore.MAX_DECODED_BYTES + 1)
        assertFalse(
            store.putBase64(
                "test:large",
                Base64.encodeToString(oversized, Base64.NO_WRAP),
            ),
        )
    }
}
