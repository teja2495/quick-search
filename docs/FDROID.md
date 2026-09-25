# F-Droid publishing guide for Quick Search

This project ships two **distribution flavors** from one codebase:

| Flavor | Gradle task | Use |
|--------|-------------|-----|
| `standard` (default) | `assembleStandardRelease` | Google Play, GitHub releases |
| `fdroid` | `assembleFdroidRelease` | [F-Droid](https://f-droid.org/) |

The `standard` flavor keeps Google Play in-app review and in-app updates. The `fdroid` flavor uses the same app with no-op stubs, does not bundle Play Core libraries, uses the system font, and defaults web suggestions to off.

## Build locally

```bash
./gradlew assembleFdroidRelease
```

APK output: `app/build/outputs/apk/fdroid/release/`

```bash
./gradlew assembleStandardRelease
```

## Upstream metadata (in this repo)

F-Droid reads listing text and graphics from:

```
fastlane/metadata/android/en-US/
```

Keep the app listing text in this `fastlane` structure. Do not duplicate summary or full description in `fdroiddata` metadata; F-Droid pulls that text from this repo.

When releasing, update `versionCode` / `versionName` in `app/build.gradle.kts`, add `changelogs/<versionCode>.txt`, and tag the release commit. If you keep the current F-Droid-specific tagging scheme, use tags like `3.7-fdroid`.

## F-Droid metadata

Quick Search is already published on F-Droid. Its build metadata lives in fdroiddata at
[`metadata/com.tk.quicksearch.yml`](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.tk.quicksearch.yml).
It auto-updates from tags matching `^([0-9.]+)-fdroid$` and declares the `NonFreeNet` anti-feature
for optional proprietary network services (AI providers, web suggestions/search integrations).
Updates only need a new tag from this repo; don't edit the fdroiddata entry unless the build or
anti-features change.

## Release checklist

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
- [ ] Tag the F-Droid release commit (current scheme: `<versionName>-fdroid`)
- [ ] `./gradlew assembleFdroidRelease` succeeds
- [ ] `./gradlew assembleStandardRelease` succeeds
- [ ] Push the tag; F-Droid discovers and builds it asynchronously

## Reproducible builds

Optional but recommended. F-Droid can verify that their APK matches yours when you publish signed release binaries and enable reproducible build metadata in fdroiddata. See https://f-droid.org/en/docs/Reproducible_Builds/
