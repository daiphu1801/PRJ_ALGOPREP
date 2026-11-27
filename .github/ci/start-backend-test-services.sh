#!/usr/bin/env bash
set -euo pipefail

minio_image=ghcr.io/projekt-manager-org/minio@sha256:a1a8bd4ac40ad7881a245bab97323e18f971e4d4cba2c2007ec1bedd21cbaba2
export ALGOPREP_WORKER_ENGINE_IMAGE=algoprep-ci-judge:test

docker pull "$minio_image"
# Testcontainers declarations use the original upstream tag.
docker tag "$minio_image" minio/minio:RELEASE.2025-09-07T16-13-09Z
docker build -t "$ALGOPREP_WORKER_ENGINE_IMAGE" 05-coding/judge-engine
docker compose -f 05-coding/docker-compose.worker-sandbox.yml up --wait --wait-timeout 120
