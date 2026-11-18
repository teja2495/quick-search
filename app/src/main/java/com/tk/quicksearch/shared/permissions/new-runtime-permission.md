# Add a runtime permission row

For an ordinary Android runtime grant, add an entry to `RuntimePermissionRow.kt` with the
permission, position, label, description, icon, and visibility rule. The shared card handles
initial state, permission request, denial, settings fallback, and refresh on resume. Provide
resource-backed label and description callbacks, and declare the permission in the Android manifest.

Wire the feature's own permission-dependent behavior where it consumes the grant. The app's
settings-screen request helper can use the permission constant from the same definition.
Permissions managed by Android settings, such as file access and accessibility, have separate
request flows.
