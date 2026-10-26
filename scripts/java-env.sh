#!/usr/bin/env bash
# 호출자의 JAVA_HOME을 우선 사용한다. 프로젝트에 내려받은 개발 JDK는 Git에 포함하지 않는다.
set -euo pipefail
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x .tools/amazon-corretto-21.jdk/Contents/Home/bin/java ]]; then
    export JAVA_HOME="$PWD/.tools/amazon-corretto-21.jdk/Contents/Home"
  elif [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  else
    echo 'Java 21을 설치하고 JAVA_HOME을 지정해 주세요.' >&2
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
