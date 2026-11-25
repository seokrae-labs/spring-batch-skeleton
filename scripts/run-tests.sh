#!/usr/bin/env bash
set -uo pipefail

cat >/dev/null

root="$(cd "$(dirname "$0")/.." && pwd)"
state="$root/build/.run-tests-passed"

fingerprint=""
if git -C "$root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fingerprint=$(
    {
      git -C "$root" rev-parse HEAD 2>/dev/null
      git -C "$root" status --porcelain
      git -C "$root" diff HEAD 2>/dev/null
    } | cksum
  )
fi

if [ -n "$fingerprint" ] && [ -f "$state" ] && [ "$(cat "$state")" = "$fingerprint" ]; then
  exit 0
fi

if ! output=$(cd "$root" && ./gradlew -q test 2>&1); then
  printf '%s\n' "$output" | tail -50 >&2
  echo "run-tests.sh: tests failed, fix them before finishing" >&2
  exit 2
fi

if [ -n "$fingerprint" ]; then
  mkdir -p "$(dirname "$state")"
  printf '%s' "$fingerprint" > "$state"
fi

exit 0
