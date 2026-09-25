---
name: device-verify
description: Install the Quick Search debug build on the attached Android device and launch it. Use only when the user explicitly asks for on-device verification, installing, or testing on their phone.
---

# On-device verification

Only do this when the user asked for it in this conversation. Bug reports are often from other users' devices, so a local install is not the default.

1. Build, install, restart and launch the debug app (`com.tk.quicksearch.debug`, "QS Debug"). It is separate from the user's installed app but holds their real test data.

```bash
./gradlew assembleStandardDebug && adb install --user 0 -r app/build/outputs/apk/standard/debug/app-standard-debug.apk && adb shell am force-stop com.tk.quicksearch.debug && adb shell am start -W -n com.tk.quicksearch.debug/com.tk.quicksearch.app.MainActivity
```

2. Both `adb install` and `am force-stop` revoke the app's accessibility service grant (edge gesture, lock action). Check it with:

```bash
adb shell settings get secure enabled_accessibility_services
```

   If the grant is gone and the task needs it, ask the user to re-enable it. Don't write that setting over adb.

3. Undo any settings or data you changed for the test.
4. Report only what you actually observed on the device. A successful install proves nothing about the fix.
