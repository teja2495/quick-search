# Add a built-in widget

Implement the widget and its receiver in the feature package. Add one entry to `BuiltInWidget.kt`
with the receiver, widget factory, and default size. Supply a configuration composable for a
custom screen or reuse a shared configuration variant. Use `refreshBuiltInWidget` from any update
source the feature owns. Supply a redraw callback only for a widget with its own sizing or drawing
path.

Register the receiver and provider XML in the Android manifest, using the appropriate update and
configuration activities. Verify receiver lookup, then manually check adding, reconfiguring, and
refreshing the widget on a launcher.
