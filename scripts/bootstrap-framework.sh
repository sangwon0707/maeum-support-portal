#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
framework_version=5.0.2
framework_commit=8f47a567ec4a8d3969f423c195115648cf562ae0
framework_source=.ref/egovframe/runtime
if [[ ! -d "$framework_source/.git" ]]; then
  git clone --depth 1 --branch v5.0.2-Final https://github.com/eGovFramework/egovframe-runtime.git "$framework_source"
fi
if [[ "$(git -C "$framework_source" rev-parse HEAD)" != "$framework_commit" ]]; then
  echo '공식 소스 커밋이 기록과 다릅니다. .docs/references/SOURCE_PROVENANCE.md를 확인해 주세요.' >&2
  exit 1
fi
# 공식 배포 저장소 접근이 어려운 환경에서도 동일한 공식 태그로 재현할 수 있게 한다.
mvn -B -f "$framework_source/pom.xml" \
  -pl Presentation/org.egovframe.rte.ptl.mvc,Persistence/org.egovframe.rte.psl.dataaccess,Foundation/org.egovframe.rte.fdl.cmmn \
  -am install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
