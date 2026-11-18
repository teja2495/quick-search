# Add a configurable gesture

Add a `HomeGesture` entry in `GestureDefinitions.kt` with a stable preference prefix, title,
icon, actions, and default. Use the notch prefix for a cutout gesture. Home and cutout settings
generate their rows, and Settings search opens the same picker. Action, custom action, and alias
keys derive from the prefix. Keep existing prefixes and action enum names when updating a gesture.

Connect recognition to the appropriate input surface, such as the Home route or the notch overlay,
and dispatch the saved action there. `SwipeDirection` defines the existing directional settings,
including the directions offered by the keyboard picker. Add new strings using the localization
guide and run `scripts/verify.sh`.
