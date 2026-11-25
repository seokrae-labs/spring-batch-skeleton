#!/usr/bin/env bash
set -uo pipefail

cat >/dev/null

root="$(cd "$(dirname "$0")/.." && pwd)"

if ! output=$(cd "$root" && ./gradlew -q test 2>&1); then
  printf '%s\n' "$output" | tail -50 >&2
  echo "run-tests.sh: tests failed, fix them before finishing" >&2
  exit 2
fi

exit 0
