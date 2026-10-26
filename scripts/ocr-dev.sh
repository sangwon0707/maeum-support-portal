#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -f .env.ocr ]]; then
  set -a
  source .env.ocr
  set +a
fi
if [[ -f .env.local ]]; then
  set -a
  source .env.local
  set +a
fi
if [[ -f .env.ai.local ]]; then
  set -a
  source .env.ai.local
  set +a
fi
: "${OCR_API_TOKEN:?32자 이상의 내부 API 토큰을 .env.ocr에 설정해 주세요.}"
./scripts/build-pdf-engine.sh
export PADDLE_PDX_CACHE_HOME="$PWD/.local/ocr-models"
export PADDLE_PDX_MODEL_SOURCE=BOS
exec .tools/ocr-venv/bin/uvicorn api:app --app-dir ocr --host 127.0.0.1 --port 8091 --no-access-log
