package com.tk.quicksearch.widgets.customButtonsWidget

import com.tk.quicksearch.media.MediaCommand
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetCommandDefinitionTest {
    @Test
    fun legacyMediaPayloadRoundTripsWithTheSameKeys() {
        val id = MediaCommand.PLAY_PAUSE.value
        val raw = """{"type":"media","mediaCommand":"$id","title":"Play or pause","customIconBase64":"icon"}"""
        val command = CustomWidgetButtonAction.fromJson(raw) as CustomWidgetButtonAction.Command
        assertEquals(CustomWidgetButtonType.MEDIA, command.type)
        assertEquals(id, command.commandId)
        assertEquals("Play or pause", command.displayLabel())
        assertEquals("icon", command.customIconBase64)

        val encoded = JSONObject(command.toJson())
        assertEquals("media", encoded.getString("type"))
        assertEquals(id, encoded.getString("mediaCommand"))
        assertEquals(command, CustomWidgetButtonAction.fromJson(encoded.toString()))
    }

    @Test
    fun unknownCommandsDoNotResolve() {
        assertNull(CustomWidgetButtonAction.fromJson("""{"type":"media","mediaCommand":"unknown"}"""))
    }
}
