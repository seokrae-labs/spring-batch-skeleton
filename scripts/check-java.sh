#!/usr/bin/env bash
set -uo pipefail

input=$(cat)

if ! command -v jq >/dev/null 2>&1; then
  echo "check-java.sh: jq is not installed, skipping checks" >&2
  exit 0
fi

file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

case "$file" in
  */src/*.java) ;;
  *) exit 0 ;;
esac

[ -f "$file" ] || exit 0

root="$(cd "$(dirname "$0")/.." && pwd)"

code_lines() {
  grep -vE '^[[:space:]]*(//|/?\*)' "$1"
}

if [[ "$file" == */src/main/* ]]; then
  if code_lines "$file" | grep -nE '@([A-Za-z_][A-Za-z0-9_]*\.)*(Autowired|Inject|Resource)([^A-Za-z0-9_]|$)' >&2; then
    echo "$file: use constructor injection, remove @Autowired/@Inject/@Resource (AGENTS.md)" >&2
    exit 2
  fi
fi

case "$file" in
  */src/test/*Test.java | */src/test/*Tests.java | */src/test/*IT.java)
    tests=$(code_lines "$file" | grep -cE '@(Test|ParameterizedTest|RepeatedTest|TestFactory)([^A-Za-z0-9_]|$)')
    names=$(code_lines "$file" | grep -cE '@DisplayName([^A-Za-z0-9_]|$)')
    if (( tests > names )); then
      echo "$file: $tests test methods but only $names @DisplayName (AGENTS.md)" >&2
      exit 2
    fi
    ;;
esac

if ! output=$(cd "$root" && ./gradlew -q compileJava compileTestJava 2>&1); then
  printf '%s\n' "$output" | tail -50 >&2
  exit 2
fi

exit 0
