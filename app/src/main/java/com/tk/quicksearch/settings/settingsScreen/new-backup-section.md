# Add a backup section

Add one `SettingsBackupManager.ExportItem` entry. The entry name is stored in the backup and
should stay stable. Supply a `BackupSectionOption` for a picker row with an availability predicate.
Set `defaultSelected = false` when the user should explicitly opt in.

- For preference-backed data, give the entry a `matchPreference` predicate. The same ownership
  rule applies to export and selective restore. Specific matchers run in declaration order before
  general Settings. Search history shows how to exclude a section from legacy full restores.
- For a standalone list, supply a `BackupSectionPayload` with a stable JSON field and export and
  restore callbacks. Bookmarks show how to use an existing codec this way. Older backups that
  lack the field leave the current list intact.
- Keep sensitive data behind explicit selection. Preserve existing field names when extending a
  section. Cover export selection, selective restore, and legacy inputs in unit tests.

Notes keep their legacy preference snapshot for older builds. Restore hooks that cancel or
reschedule platform alarms stay attached to that lifecycle.
