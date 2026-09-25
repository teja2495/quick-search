#!/usr/bin/env bash
# One-shot verification for a finished change. Run from anywhere in the repo.
#
#   scripts/verify.sh               compile standard flavor, unit tests, checks, debug APK
#   scripts/verify.sh --no-assemble same, but skip building the APK (and the device install)
#   scripts/verify.sh --no-device   build the APK but don't install it on a connected device
#
# When one device is connected (or ANDROID_SERIAL picks one), the debug APK is installed and
# launched there after a clean build. The install revokes the app's accessibility grant.
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

tasks=(
    :app:compileStandardDebugKotlin
    :app:testStandardDebugUnitTest
)
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

installed=false
debug_package=com.tk.quicksearch.debug
apk=app/build/outputs/apk/standard/debug/app-standard-debug.apk
if $assemble && $device && ((${#failures[@]} == 0)) && command -v adb >/dev/null; then
    connected=$(adb devices | awk 'NR > 1 && $2 == "device"' | wc -l | tr -d ' ')
    if [[ -n "${ANDROID_SERIAL:-}" || "$connected" == 1 ]]; then
        echo "==> Install and launch on device"
        if adb install --user 0 -r "$apk" | cap 10 &&
            adb shell am force-stop "$debug_package" &&
            adb shell am start -W -n "$debug_package/com.tk.quicksearch.app.MainActivity" | cap 10; then
            installed=true
        else
            failures+=("device install")
        fi
    elif ((connected > 1)); then
        echo "==> Skipping device install: $connected devices connected, set ANDROID_SERIAL to pick one"
    fi
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
    echo "Installed and launched $debug_package on the device."
    if ! adb shell settings get secure enabled_accessibility_services | grep -q "$debug_package"; then
        echo "Accessibility service grant is off; re-enable it in Settings > Accessibility if needed."
    fi
fi
