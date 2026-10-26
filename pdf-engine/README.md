# 마음이음 PDF 처리 모듈

Apache PDFBox 3.0.8을 사용하는 Java 모듈이다. PDF 원문과 좌표, 페이지 렌더링, 이미지가 배치된 영역, PDF 분할·스캔 사본 생성을 담당한다. Python OCR API와 준비 스크립트가 같은 모듈을 사용한다.

## 빌드·검증

```bash
./scripts/build-pdf-engine.sh
mvn -f pdf-engine/pom.xml test
```

Java 17 이상이 필요하다. 웹 애플리케이션의 Java 21과 함께 사용할 수 있다. 실행 JAR은 빌드 산출물이므로 Git에 포함하지 않는다. OCR 컨테이너는 다단계 빌드로 모듈을 빌드하고 Java 런타임을 포함한다.

## 프로세스 계약

```bash
java -Xmx128m -Djava.awt.headless=true -jar pdf-engine/target/pdf-engine.jar validate 문서.pdf
java -Xmx128m -Djava.awt.headless=true -jar pdf-engine/target/pdf-engine.jar render 문서.pdf 120
java -Xmx128m -Djava.awt.headless=true -jar pdf-engine/target/pdf-engine.jar split 문서.pdf 1,2
java -Xmx128m -Djava.awt.headless=true -jar pdf-engine/target/pdf-engine.jar scan 문서.pdf 사본.pdf
```

- validate: 페이지 수 JSON, 암호화·손상·허용 범위를 검증한다.
- render: 메타데이터 다음 페이지별 NDJSON. 픽셀 좌표의 텍스트·이미지 영역과 PNG 미리보기를 반환한다.
- split: 선택한 페이지를 순서대로 묶은 PDF 바이트를 표준 출력으로 반환한다. 같은 원본·선택에 문서 ID를 고정한다.
- scan: 원본의 기울기·그림자를 포함한 페이지를 렌더링하여 이미지 PDF 사본을 생성한다.

10MiB·1~12쪽·페이지 크기·이미지 픽셀을 제한한다. Python 호출기는 인자를 배열로 전달하며 셸을 사용하지 않는다. 임시 원본 파일은 접근 권한 600으로 만들고 정상·실패 시 정리한다. 렌더링은 페이지 단위로 읽고, JVM 힙 128MiB와 응답 대기 제한을 적용한다. OCR 처리 중 대기하는 시간은 PDF 응답 제한에 포함하지 않는다.

이 모듈은 HTTP 인증·신청 권한·업무 상태·DB를 처리하지 않는다. 해당 책임은 기존 Java Controller·Service·Policy에 있다.

## 라이선스

PDFBox와 의존 라이브러리의 고지는 패키지 JAR의 LICENSE·NOTICE에 유지한다. 프로젝트 전체 라이선스와 외부 구성요소의 라이선스는 별개다. [Third-party notices](../THIRD_PARTY_NOTICES.md)를 함께 확인한다.
