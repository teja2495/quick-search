#!/usr/bin/env bash
# Claude Code Stop hook: cheap checks on uncommitted changes (no Gradle).
# Exit 2 sends the problems back to the agent once; stop_hook_active prevents a loop.
set -uo pipefail

cd "$(dirname "$0")/.."

input=$(cat)
if printf '%s' "$input" | grep -q '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
    exit 0
fi

# Nothing changed, nothing to check.
if [[ -z "$(git status --porcelain)" ]]; then
    exit 0
fi

problems=""
if ! out=$(git diff --check HEAD 2>&1); then
    problems+="git diff --check found whitespace problems:"$'\n'"$out"$'\n'
fi
untracked_ws=$(git ls-files -z --others --exclude-standard | xargs -0 grep -nIE '[[:space:]]+$' 2>/dev/null || true)
if [[ -n "$untracked_ws" ]]; then
    problems+="Trailing whitespace in new files:"$'\n'"$untracked_ws"$'\n'
fi
if ! out=$(python3 scripts/check_strings.py 2>&1); then
    problems+="$out"$'\n'
fi

if [[ -n "$problems" ]]; then
    printf '%s' "$problems" >&2
    echo "Fix these, then run scripts/verify.sh before finishing." >&2
    exit 2
fi
exit 0
