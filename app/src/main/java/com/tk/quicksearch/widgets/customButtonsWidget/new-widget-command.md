# Add a widget or gesture command

For a fixed command, register a `WidgetCommandDefinition` on a `CustomWidgetButtonType` entry.
Each choice declares its stable ID, label, searchable words, icon, and execution callback.
The entry supplies picker results and its filter, saved JSON, icons, and dispatch on the widget,
gesture, and notification surfaces. Preserve existing type values, parameter names, and command
IDs. Media in `WidgetCommandDefinition.kt` shows how to supply a live icon as well as a static one.

Device content such as a contact, note, or file has its own editable payload and uses the
content-backed action path.
