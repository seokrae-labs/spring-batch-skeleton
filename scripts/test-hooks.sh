#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
main_dir="$root/batch-job-sample/src/main/java/com/example/batch/job/sample"
test_dir="$root/batch-job-sample/src/test/java/com/example/batch/job/sample"
created=()

cleanup() {
  if [ "${#created[@]}" -gt 0 ]; then
    rm -f "${created[@]}"
  fi
}
trap cleanup EXIT

write_file() {
  created+=("$1")
  printf '%s\n' "$2" > "$1"
}

expect_exit() {
  local expected=$1 label=$2 file=$3
  local actual=0
  printf '{"tool_input":{"file_path":"%s"}}' "$file" \
    | "$root/scripts/check-java.sh" > /dev/null 2>&1 || actual=$?
  if [ "$actual" -ne "$expected" ]; then
    echo "FAIL $label: expected exit $expected, got $actual" >&2
    exit 1
  fi
  echo "ok   $label (exit $actual)"
}

write_file "$main_dir/HookFieldInjection.java" \
  'package com.example.batch.job.sample;
class HookFieldInjection { @org.springframework.beans.factory.annotation.Autowired Object field; }'
expect_exit 2 "field injection in main code is rejected" "$main_dir/HookFieldInjection.java"
rm -f "$main_dir/HookFieldInjection.java"

write_file "$main_dir/HookJavadoc.java" \
  'package com.example.batch.job.sample;
/**
 * Mentions @Autowired only in a comment.
 */
class HookJavadoc {}'
expect_exit 0 "annotations in comments are ignored" "$main_dir/HookJavadoc.java"
rm -f "$main_dir/HookJavadoc.java"

write_file "$test_dir/HookMissingNameTest.java" \
  'package com.example.batch.job.sample;
import org.junit.jupiter.api.Test;
class HookMissingNameTest { @Test void a() {} }'
expect_exit 2 "test without @DisplayName is rejected" "$test_dir/HookMissingNameTest.java"
rm -f "$test_dir/HookMissingNameTest.java"

expect_exit 0 "existing test passes" "$test_dir/SampleJobTest.java"
expect_exit 0 "non-Java files are skipped" "$root/README.md"

if printf '{}' | "$root/scripts/run-tests.sh"; then
  echo "ok   run-tests.sh passes on the current tree"
else
  echo "FAIL run-tests.sh" >&2
  exit 1
fi
