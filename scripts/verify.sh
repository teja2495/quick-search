#!/usr/bin/env bash
# One-shot verification for a finished change. Run from anywhere in the repo.
#
#   scripts/verify.sh               compile standard flavor, run checks, build debug APK
#   scripts/verify.sh --no-assemble same, but skip building the APK (and the device install)
#   scripts/verify.sh --no-device   build the APK but don't install it on a connected device
#
# After a clean build, install and launch on all USB devices if any are connected, otherwise
# all wireless devices, skipping Android TVs when ~/.local/bin/android-targets is installed.
# ANDROID_SERIAL overrides this selection. Installation revokes accessibility.
# Unit tests run when app/build.gradle.kts changes versionCode relative to HEAD.
#
# Extra arguments after the flags are passed to Gradle (e.g. --offline).
# Output is kept short: Gradle's full log goes to build/verify-gradle.log, and on failure only
# compiler errors, failing tests, and Gradle's error summary are printed.
set -euo pipefail

cd "$(dirname "$0")/.."

MAX_KOTLIN_LINES=800
assemble=true
device=true
gradle_args=()
for arg in "$@"; do
    case "$arg" in
        --no-assemble) assemble=false ;;
        --no-device) device=false ;;
        *) gradle_args+=("$arg") ;;
    esac
done

failures=()

# Prints at most $1 lines of stdin, then says how many were cut.
cap() {
    awk -v max="$1" 'NR <= max { print } END { if (NR > max) print "  ... " NR - max " more lines" }'
}

echo "==> Whitespace (git diff --check)"
untracked_ws=$(git ls-files -z --others --exclude-standard | xargs -0 grep -nIE '[[:space:]]+$' 2>/dev/null || true)
if ! git diff --check HEAD | cap 40 || [[ -n "$untracked_ws" ]]; then
    [[ -n "$untracked_ws" ]] && echo "Trailing whitespace in new files:" && echo "$untracked_ws" | cap 40
    failures+=("whitespace")
fi

echo "==> String resource parity"
if ! python3 scripts/check_strings.py | cap 40; then
    failures+=("string parity")
fi

echo "==> Kotlin file size (max $MAX_KOTLIN_LINES lines)"
oversized=$(find app/src -name '*.kt' -path '*/java/*' -exec wc -l {} + |
    awk -v max="$MAX_KOTLIN_LINES" '$2 != "total" && $1 > max { print "  " $1 " " $2 }')
if [[ -n "$oversized" ]]; then
    echo "Split these into focused files or delegates:"
    echo "$oversized"
    failures+=("file size")
fi

read_version_code() {
    sed -nE 's@^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+)[[:space:]]*(//.*)?$@\1@p' "$@"
}

tasks=(:app:compileStandardDebugKotlin)
app_gradle=app/build.gradle.kts
head_version_code=$(git show "HEAD:$app_gradle" | read_version_code)
version_code=$(read_version_code "$app_gradle")
if [[ ! "$head_version_code" =~ ^[0-9]+$ || ! "$version_code" =~ ^[0-9]+$ ]]; then
    echo "VERIFY FAILED: expected one numeric versionCode in $app_gradle and in HEAD"
    exit 1
fi
if [[ "$head_version_code" != "$version_code" ]]; then
    echo "==> Unit tests: versionCode changed ($head_version_code to $version_code)"
    tasks+=(:app:testStandardDebugUnitTest)
else
    echo "==> Unit tests skipped: versionCode unchanged ($version_code)"
fi
if $assemble; then
    tasks+=(assembleStandardDebug)
fi

# Pulls the actionable part out of a failed Gradle log.
report_gradle_failure() {
    local log=$1 marker=$2
    grep -E '^e: ' "$log" | sed "s#file://$PWD/##" | awk '!seen[$0]++' | cap 40 || true
    python3 - "$marker" <<'EOF' | cap 60
import glob, os, sys
import xml.etree.ElementTree as ET
since = os.path.getmtime(sys.argv[1])
for path in sorted(glob.glob("app/build/test-results/*/TEST-*.xml")):
    if os.path.getmtime(path) < since:
        continue
    for case in ET.parse(path).getroot().iter("testcase"):
        for problem in list(case.findall("failure")) + list(case.findall("error")):
            print(f"FAILED {case.get('classname')} > {case.get('name')}")
            detail = (problem.get("message") or problem.text or "").strip().splitlines()
            for line in detail[:6]:
                print("    " + line)
EOF
    awk '/^\* What went wrong:/ { on = 1 } /^\* Try:/ { on = 0 } on && NF' "$log" | cap 20
    echo "Full Gradle log: $log"
}

echo "==> Gradle: ${tasks[*]}"
mkdir -p build
gradle_log=build/verify-gradle.log
gradle_marker=build/verify-gradle.started
touch "$gradle_marker"
if ! ./gradlew -q --console=plain "${tasks[@]}" ${gradle_args[@]+"${gradle_args[@]}"} >"$gradle_log" 2>&1; then
    report_gradle_failure "$gradle_log" "$gradle_marker"
    failures+=("gradle")
fi

# Device selection is shared with ~/.zshrc's run/release via ~/.local/bin/android-targets
# (skips Android TVs, prefers USB, otherwise every wireless target, honors ANDROID_SERIAL).
# Without that helper, fall back to: ANDROID_SERIAL, else USB first, else wireless.
android_target_serials() {
    if command -v android-targets >/dev/null; then
        android-targets mobile
        return
    fi
    if [[ -n "${ANDROID_SERIAL:-}" ]]; then
        printf '%s\n' "$ANDROID_SERIAL"
        return
    fi
    adb devices -l | awk '
        NR > 1 && $2 == "device" {
            usb = 0
            for (i = 3; i <= NF; i++) if ($i ~ /^usb:/) usb = 1
            if (usb) wired[++w] = $1; else wireless[++n] = $1
        }
        END {
            if (w) for (i = 1; i <= w; i++) print wired[i]
            else for (i = 1; i <= n; i++) print wireless[i]
        }
    '
}

installed=false
installed_serials=()
debug_package=com.tk.quicksearch.debug
apk=app/build/outputs/apk/standard/debug/app-standard-debug.apk
if $assemble && $device && ((${#failures[@]} == 0)) && command -v adb >/dev/null; then
    target_serials=$(android_target_serials)
    while IFS= read -r device_serial; do
        [[ -n "$device_serial" ]] || continue
        echo "==> Install and launch on $device_serial"
        if adb -s "$device_serial" install --user 0 -r "$apk" | cap 10 &&
            adb -s "$device_serial" shell am force-stop "$debug_package" &&
            adb -s "$device_serial" shell am start -W -n "$debug_package/com.tk.quicksearch.app.MainActivity" | cap 10; then
            installed=true
            installed_serials+=("$device_serial")
        else
            failures+=("device install ($device_serial)")
        fi
    done <<< "$target_serials"
fi

if ((${#failures[@]})); then
    echo
    echo "VERIFY FAILED: ${failures[*]}"
    exit 1
fi

echo
echo "VERIFY PASSED"
if $assemble; then
    echo "APK: $apk"
fi
if $installed; then
    for device_serial in "${installed_serials[@]}"; do
        echo "Installed and launched $debug_package on $device_serial."
        if ! adb -s "$device_serial" shell settings get secure enabled_accessibility_services | grep -q "$debug_package"; then
            echo "Accessibility service grant is off on $device_serial; re-enable it in Settings > Accessibility if needed."
        fi
    done
fi
