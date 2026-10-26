#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
mkdir -p .tools
if [[ ! -f .tools/google-java-format.jar ]]; then
  curl -fsSL https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.28.0/google-java-format-1.28.0-all-deps.jar -o .tools/google-java-format.jar
fi
format_options=(--aosp --replace)
if [[ "${1:-}" == '--check' ]]; then format_options=(--aosp --dry-run --set-exit-if-changed); fi
find src/main/java src/test/java pdf-engine/src/main/java pdf-engine/src/test/java -name '*.java' -print0 | xargs -0 java -jar .tools/google-java-format.jar "${format_options[@]}"
