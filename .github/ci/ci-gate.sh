#!/usr/bin/env bash
set -euo pipefail

event=$1
if [[ $2 != success ]]; then
  echo "Change detection did not succeed: $2" >&2
  exit 1
fi
shift 2

while (( $# )); do
  job=$1
  changed=$2
  result=$3
  shift 3
  if [[ "$changed" != true && "$changed" != false ]]; then
    echo "$job: missing or invalid change classification ($changed)" >&2
    exit 1
  fi
  expected=skipped
  if [[ "$event" == workflow_dispatch || "$changed" == true ]]; then
    expected=success
  fi
  if [[ "$result" != "$expected" ]]; then
    echo "$job: expected $expected, got $result" >&2
    exit 1
  fi
  echo "$job: $result"
done
