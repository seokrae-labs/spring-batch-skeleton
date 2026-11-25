#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

fresh_copy() {
  rm -rf "$work/repo"
  mkdir -p "$work/repo"
  (cd "$root" && git ls-files -z | tar --null -T - -cf -) | tar -xf - -C "$work/repo"
}

expect_failure() {
  local label=$1 expected=$2
  shift 2
  local status=0
  (cd "$work/repo" && "$@") > "$work/output.log" 2>&1 || status=$?
  find "$work/repo" -path '*/build/test-results/*' -name '*.xml' -exec cat {} + >> "$work/output.log" 2>/dev/null || true
  if [ "$status" -eq 0 ]; then
    echo "FAIL $label: the build passed but should have failed" >&2
    exit 1
  fi
  if ! grep -qF "$expected" "$work/output.log"; then
    echo "FAIL $label: failed without the expected message: $expected" >&2
    tail -30 "$work/output.log" >&2
    exit 1
  fi
  echo "ok   $label"
}

add_module() {
  local name=$1 plugin=$2 dependencies=$3
  printf "include '%s'\n" "$name" >> "$work/repo/settings.gradle"
  mkdir -p "$work/repo/$name"
  printf "plugins {\n    id '%s'\n}\n\ndependencies {\n%s\n}\n" "$plugin" "$dependencies" > "$work/repo/$name/build.gradle"
}

fresh_copy
add_module batch-job-other batch.job "    implementation project(':batch-job-sample')"
expect_failure "job group depending on another job group" \
  "batch-job-other -> batch-job-sample" ./gradlew help

fresh_copy
printf "\ndependencies {\n    implementation project(':batch-db-main')\n}\n" >> "$work/repo/batch-core/build.gradle"
expect_failure "batch-core depending on a business database module" \
  "batch-core -> batch-db-main" ./gradlew help

fresh_copy
printf "\ndependencies {\n    implementation 'org.springframework.boot:spring-boot-starter'\n}\n" >> "$work/repo/batch-job-sample/build.gradle"
expect_failure "Spring Boot outside a launcher module" \
  "batch-job-sample -> org.springframework.boot:spring-boot-starter" ./gradlew help

fresh_copy
mkdir -p "$work/repo/settlement-batch"
printf "include 'settlement-batch'\n" >> "$work/repo/settings.gradle"
expect_failure "module name outside the naming rule" \
  "Module names must follow" ./gradlew help

fresh_copy
add_module batch-job-orders batch.db ""
expect_failure "role plugin that does not match the module name" \
  "applies batch.db but is not named batch-db-<name>" ./gradlew help

fresh_copy
perl -ni -e 'print unless /\.transactionManager\(mainTransactionManager\)/' \
  "$work/repo/batch-job-sample/src/main/java/com/example/batch/job/sample/SampleJobConfig.java"
expect_failure "step without the business transaction manager" \
  "loadSampleRecordStep uses ResourcelessTransactionManager" \
  ./gradlew :batch-job-sample:test --tests '*SampleJobTest.everyStepRunsInBusinessTransaction'

fresh_copy
cat > "$work/repo/batch-job-sample/src/main/java/com/example/batch/job/sample/RemovedApiUse.java" <<'JAVA'
package com.example.batch.job.sample;

import org.springframework.batch.core.job.JobExecution;

class RemovedApiUse {
  long instanceId(JobExecution execution) {
    return execution.getJobInstanceId();
  }
}
JAVA
expect_failure "API marked for removal" \
  "warnings found and -Werror specified" ./gradlew :batch-job-sample:compileJava
