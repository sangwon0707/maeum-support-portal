#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx192m -XX:+UseSerialGC}"
mvn -q -f pdf-engine/pom.xml -DskipTests package
