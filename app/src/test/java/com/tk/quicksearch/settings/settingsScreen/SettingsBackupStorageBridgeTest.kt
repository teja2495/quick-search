package com.tk.quicksearch.settings.settingsScreen

import java.io.StringWriter
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupStorageBridgeTest {
    @Test
    fun inlineFavicons_fillsOnlyBlankFavicons() {
        val engines =
            JSONArray()
                .put(JSONObject().put("id", "a").put("name", "A").put("faviconBase64", ""))
                .put(JSONObject().put("id", "b").put("name", "B").put("faviconBase64", "inline"))
                .put(JSONObject().put("id", "c").put("name", "C"))
                .toString()

        val result =
            JSONArray(
                SettingsBackupStorageBridge.inlineFavicons(engines) { id ->
                    mapOf("a" to "storedA", "b" to "storedB")[id]
                },
            )

        assertEquals("storedA", result.getJSONObject(0).getString("faviconBase64"))
        assertEquals("inline", result.getJSONObject(1).getString("faviconBase64"))
        assertFalse(result.getJSONObject(2).has("faviconBase64"))
    }

    @Test
    fun inlineFavicons_keepsInputWhenNothingChangesOrJsonIsInvalid() {
        val engines = JSONArray().put(JSONObject().put("id", "a").put("faviconBase64", "")).toString()
        assertSame(engines, SettingsBackupStorageBridge.inlineFavicons(engines) { null })

        val invalid = "not json"
        assertSame(invalid, SettingsBackupStorageBridge.inlineFavicons(invalid) { "icon" })
    }

    @Test
    fun writeSections_writesParseableJsonAndSkipsEmptySections() {
        val writer = StringWriter()
        SettingsBackupStorageBridge.writeSections(
            writer,
            sequenceOf(
                "empty" to emptySequence(),
                "first" to
                    sequenceOf(
                        "key \"quoted\"" to JSONObject().put("type", "string").put("value", "a\nb"),
                        "count" to JSONObject().put("type", "int").put("value", 3),
                    ),
                "alsoEmpty" to emptySequence(),
                "second" to sequenceOf("flag" to JSONObject().put("type", "boolean").put("value", true)),
            ),
        )

        val parsed = JSONObject(writer.toString())
        assertEquals(setOf("first", "second"), parsed.keys().asSequence().toSet())
        val first = parsed.getJSONObject("first")
        assertEquals("a\nb", first.getJSONObject("key \"quoted\"").getString("value"))
        assertEquals(3, first.getJSONObject("count").getInt("value"))
        assertTrue(parsed.getJSONObject("second").getJSONObject("flag").getBoolean("value"))
    }
}
