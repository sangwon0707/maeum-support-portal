#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
docker compose -p egov-portfolio up -d --wait db
# 운영·개발 데이터베이스를 지우지 않고 별도 테스트 스키마만 준비한다.
docker compose -p egov-portfolio exec -T db mariadb -uroot "-p${DB_ROOT_PASSWORD:-local-root-password}" -e \
  "CREATE DATABASE IF NOT EXISTS support_portal_test; GRANT ALL ON support_portal_test.* TO 'support_app'@'%';"
mvn -B -f pdf-engine/pom.xml package
mvn -B verify
if [[ -x .tools/ocr-venv/bin/python ]]; then
  .tools/ocr-venv/bin/python -m unittest discover -s ocr -p 'test_*.py'
fi
