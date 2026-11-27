#!/usr/bin/env bash
set -euo pipefail

gate="$(dirname "$0")/ci-gate.sh"
bash "$gate" pull_request success frontend false skipped backend false skipped infrastructure false skipped
bash "$gate" pull_request success frontend true success backend false skipped infrastructure false skipped
bash "$gate" pull_request success frontend false skipped backend true success infrastructure false skipped
bash "$gate" pull_request success frontend false skipped backend false skipped infrastructure true success
bash "$gate" workflow_dispatch success frontend false success backend false success infrastructure false success
bash "$gate" push success frontend false success backend false success infrastructure false success
if bash "$gate" push success backend false skipped; then
  echo "Gate should reject a skipped job on a full push run" >&2
  exit 1
fi

for result in failure cancelled skipped ""; do
  if bash "$gate" pull_request success backend true "$result"; then
    echo "Gate should reject required backend result: '$result'" >&2
    exit 1
  fi
done
if bash "$gate" pull_request failure || bash "$gate" pull_request success backend "" skipped; then
  echo "Gate should reject missing or failed change detection" >&2
  exit 1
fi
if bash "$gate" pull_request success frontend false success; then
  echo "Gate should reject an unexpected job run" >&2
  exit 1
fi
