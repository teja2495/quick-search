---
name: fdroid-release
description: Promote a new Quick Search Android release to F-Droid. Use when asked to send, publish, promote, or prepare the latest Quick Search update for F-Droid, especially when release notes live in app/src/main/assets/RELEASE_NOTES.md.
---

# Quick Search F-Droid release

Publish F-Droid updates through the upstream tag-driven workflow. The release tag is the handoff to F-Droid; F-Droid builds and indexes it asynchronously.

## Preconditions

1. Work only from `/Users/teja2495/Projects/quick-search`; inspect its `AGENTS.md` first.
2. Confirm `app/build.gradle.kts` contains a non-empty `versionName` and `versionCode`.
3. Read `app/src/main/assets/RELEASE_NOTES.md`. Use its content verbatim for the F-Droid changelog.
4. Check `git status --short`. Preserve unrelated worktree changes. If they overlap the release files or make the release commit ambiguous, stop and ask the user.
5. Read `docs/FDROID.md` and check the remote F-Droid metadata at `https://gitlab.com/fdroid/fdroiddata/-/raw/master/metadata/com.tk.quicksearch.yml`. Confirm it still uses the `Tags ^([0-9.]+)-fdroid$` update rule before publishing.

## Prepare the release

1. Set `release_version` to `versionName` and `release_code` to `versionCode` from `app/build.gradle.kts`.
2. Use `release_tag="$release_version-fdroid"`.
3. Verify the tag does not already exist locally or on `origin`. Do not move or overwrite an existing tag.
4. Create or update exactly `fastlane/metadata/android/en-US/changelogs/<release_code>.txt` with the contents of `RELEASE_NOTES.md`. Preserve every release-note line and ordering; do not substitute generic release copy.
5. Review the diff and run `git diff --check`.

## Validate before publishing

Run both checks successfully. Do not publish if either fails.

```bash
./gradlew assembleFdroidRelease
```

Confirm the APK exists at `app/build/outputs/apk/fdroid/release/app-fdroid-release.apk`.

Then run Quick Search's required device verification exactly:

```bash
./gradlew assembleStandardDebug && adb install --user 0 -r app/build/outputs/apk/standard/debug/app-standard-debug.apk && adb shell am force-stop com.tk.quicksearch && adb shell am force-stop com.tk.quicksearch.debug && adb shell am start -W -n com.tk.quicksearch.debug/com.tk.quicksearch.app.MainActivity
```

If an Android device is unavailable or either check fails, report the exact blocker and do not tag or push.

## Publish

Only commit and push when the user explicitly asks to send, publish, or promote the update. The phrase "use `$fdroid-release` to promote …" is sufficient authorization.

1. Commit only the changelog, using `Add F-Droid changelog for v<release_version>`.
2. Create an annotated tag named `<release_version>-fdroid` with message `F-Droid-ready release (flavor fdroid, version <release_version> / <release_code>)`.
3. Push `main`, then push the new tag to `origin`.
4. Verify the remote tag with `git ls-remote --tags origin refs/tags/<release_tag>` and confirm the tagged tree contains the changelog.
5. Report the version, version code, commit, tag, validation results, and that F-Droid will discover/build the tag asynchronously. Do not claim the app is already live until F-Droid has actually published it.

## Safety rules

- Never alter an existing F-Droid tag, bump versions, or edit F-Droid's central metadata without explicit user direction.
- Never publish if the remote update rule, version/tag relationship, changelog, or validation result is uncertain.
- Do not use the standard Google Play/GitHub release tag as the F-Droid tag; publish the separate `-fdroid` tag.
