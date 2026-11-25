#!/usr/bin/env bash
set -euo pipefail

base_ref=${1:?usage: flag-guarded-changes.sh <base-ref>}

guarded='^(buildSrc/|scripts/|\.github/workflows/|\.claude/|AGENTS\.md$|settings\.gradle$|\.gitattributes$)'

changed=$(git diff --name-only "$(git merge-base "$base_ref" HEAD)" HEAD | grep -E "$guarded" || true)

if [ -z "$changed" ]; then
  echo "No changes to build rules, checks, or agent rules."
  exit 0
fi

summary=${GITHUB_STEP_SUMMARY:-/dev/stdout}
{
  echo "### Changes to build rules, checks, or agent rules"
  echo
  echo "These files decide what the build and the coding agents are allowed to do. Review them by hand before merging."
  echo
  while IFS= read -r file; do
    echo "- \`$file\`"
  done <<< "$changed"
} >> "$summary"

while IFS= read -r file; do
  echo "::warning file=$file::This file defines build rules, checks, or agent rules. Review the change by hand."
done <<< "$changed"
