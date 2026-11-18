# Add an At a Glance device status

Use this path for a device status that can be read when Home resumes and when a widget refreshes.
Low storage is the example in `search/glance/LowStorageStatus.kt`.

1. Define the preference key, title, description, icon, and sampling function in a
   `GlanceStatusDefinition`. The reader returns a `GlanceStatusValue` when the status should
   appear. It runs off the main thread.
2. Add a `GlanceItem` entry with a stable widget override key and the definition as `status`.
   Settings, Home, and the widget discover the entry. Provide open and dismiss callbacks if the
   row needs those actions.
3. Localize new strings and test the status policy. Check Home and a widget with an explicit
   override during manual device testing.

Live notification feeds, scheduled items, and sources that need their own observers or permissions
keep that lifecycle in the feature.
