#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
docker compose -p egov-portfolio up -d --wait db
if [[ ! -f "${MAVEN_USER_HOME:-$HOME/.m2}/repository/org/egovframe/rte/egovframe-rte-ptl-mvc/5.0.2/egovframe-rte-ptl-mvc-5.0.2.jar" ]]; then
  ./scripts/bootstrap-framework.sh
fi
mvn -B compile exec:java
