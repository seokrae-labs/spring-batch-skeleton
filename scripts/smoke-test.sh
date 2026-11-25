#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

printf 'a,100\nb,0\nc,300\n' > "$work/2026-11-25.csv"
base_path="-Djob-sample.base-path=$work"

cli=("$java_cmd" "$base_path" -cp "batch-job-sample/build/runtime/*"
  org.springframework.batch.core.launch.support.CommandLineJobOperator
  com.example.batch.job.sample.SampleJobGroupBootstrap)
launcher=("$java_cmd" "$base_path" -jar batch-launcher-cli/build/libs/batch-launcher-cli-0.1.0.jar)

expect_exit() {
  local expected=$1 label=$2
  shift 2
  local actual=0
  "$@" > "$work/output.log" 2>&1 || actual=$?
  if [ "$actual" -ne "$expected" ]; then
    echo "FAIL $label: expected exit $expected, got $actual" >&2
    tail -40 "$work/output.log" >&2
    exit 1
  fi
  echo "ok   $label (exit $actual)"
}

expect_exit 0 "CommandLineJobOperator completes" \
  "${cli[@]}" start sampleJob targetDate=2026-11-25,java.time.LocalDate
expect_exit 0 "launcher completes" \
  "${launcher[@]}" --job.name=sampleJob targetDate=2026-11-25,java.time.LocalDate
expect_exit 1 "launcher fails when the input file is missing" \
  "${launcher[@]}" --job.name=sampleJob targetDate=2026-11-26,java.time.LocalDate
expect_exit 2 "launcher rejects a missing job name" \
  "${launcher[@]}" targetDate=2026-11-25,java.time.LocalDate
expect_exit 2 "launcher rejects an unknown job name" \
  "${launcher[@]}" --job.name=unknownJob targetDate=2026-11-25,java.time.LocalDate
