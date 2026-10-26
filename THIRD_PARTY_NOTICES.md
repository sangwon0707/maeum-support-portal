# Third-Party Notices

외부 구성요소·글꼴·생성 자료의 저작권, 라이선스와 출처를 기록한다. 이 문서는 자체 코드에 일괄 라이선스를 부여하지 않으며 각 구성요소의 원문 조건을 대체하지 않는다.

갱신일: 2026-10-26. 프로젝트 코드·고정 출처·로컬 패키지 메타데이터를 기준으로 작성했다. Git에 포함한 자산과 Maven·Python 의존성의 확인 결과를 구분한다. 기반 OS 패키지 전체의 법률 감사를 완료했다는 의미는 아니다.

## 서비스에 적용한 외부 기술

| 항목 | 사용 범위 | 출처·고지 |
| --- | --- | --- |
| eGovFrame 실행환경 5.0.2 | MVC·데이터 접근·공통 모듈 | Apache License 2.0. `src/main/resources/META-INF/licenses/egovframe-LICENSE.txt`, [공식 저장소](https://github.com/eGovFramework/egovframe-runtime). 태그 `v5.0.2-Final`, 커밋 `8f47a567ec4a8d3969f423c195115648cf562ae0` |
| KRDS HTML Component Kit 1.1.0 | CSS·글꼴·아이콘 | [공식 저장소](https://github.com/KRDS-uiux/krds-uiux), 커밋 `d6bb184c823e4757f05807ea4646a23e3133b6e6`. 원본 package.json에는 ISC가 기재되어 있지만 자료 전체의 조건으로 단정하지 않는다. KRDS 공식 저작권 안내의 공공누리 제1유형과 개별 리소스·서체의 [공식 이용 안내](https://www.krds.go.kr/html/site/outline/outline_05.html)및 동봉 고지를 함께 확인한다 |
| Pretendard GOV | 웹 UI 폰트 6개 | SIL Open Font License 1.1. [라이선스 전문](src/main/webapp/assets/krds/fonts/OFL.txt)·[폰트 저작권](src/main/webapp/assets/krds/fonts/NOTICE.txt), [upstream v1.3.9](https://github.com/orioncactus/pretendard/blob/v1.3.9/LICENSE). Copyright Kil Hyung-jin. 원본 폰트 이름과 고지를 보존한다 |
| Swiper 11.0.6 | KRDS에 포함된 슬라이더 CSS | MIT. Copyright Vladimir Kharlampidi. [라이선스 전문](src/main/webapp/assets/krds/css/plugin/LICENSE.swiper.txt), [upstream](https://github.com/nolimits4web/swiper/blob/v11.0.6/LICENSE). CSS 원본 헤더를 보존한다 |
| Noto Sans KR | 문서·가상 서류 글꼴 | SIL Open Font License 1.1, `src/main/resources/fonts/OFL.txt`. [Google Fonts 원본](https://github.com/google/fonts/tree/main/ofl/notosanskr) |
| Java 라이브러리 | Spring·Security·MyBatis·MariaDB JDBC·Flyway·Jackson 등 | 직접 의존성과 고정 버전은 `pom.xml`, 전이 의존성은 Maven dependency tree로 확인. 모두 본 프로젝트 자체 개발물이라고 표시하지 않는다 |

KRDS 제품용 CSS의 외부 이미지 경로는 로컬 상대 경로로 변경했다. 공식 저장소의 원본은 보존했다. 공식 공통컴포넌트 전체를 복제한 서비스가 아니며 eGovFrame·KRDS의 인증·공식 운영을 주장하지 않는다. 업무 서비스·SQL·JSP·문서 생성 코드는 이 프로젝트에서 작성했다.

## 문서 제작·검토 도구

문서 제작 환경에서 확인한 버전이며 Java 서비스의 런타임 의존성과 구분한다.

| 도구 | 확인 버전 | 로컬 메타데이터의 라이선스 표기 | 용도 |
| --- | --- | --- | --- |
| ReportLab | 5.0.1 | BSD 계열, 패키지 license.txt 참조 | PDF 생성 |
| pypdf | 6.16.2 | BSD-3-Clause | 쪽수·텍스트 층·이미지 검사 |
| fontTools | 4.60.1 | MIT | 가변 글꼴의 정적 굵기 생성 |
| Pillow | 12.3.0 | MIT-CMU | 이미지 크기·색상 검사 |
| olefile | 0.47 | BSD | 참고 HWP 본문 추출 |
| Poppler CLI | 로컬 설치본 | 재배포 시 설치본 라이선스 별도 확인 | pdftoppm·pdftotext·pdfinfo로 변환·검토 |

Python 도구는 `.tools/documents-venv`에 구성했다. 패키지 자체를 이 저장소에 복사·배포하는 방식은 사용하지 않는다.

## 참고 자료와 자체 자료의 구분

- 수원시·전북·충주시 신청 서식은 항목·표·사진대지 구조를 검토하는 참고 자료다. 원본·출처·해시·비교 기록은 `.archive/예시문서-이전버전/참고서식/`에 보관한다. 기관 서식을 서비스 리소스로 배포하지 않는다.
- 제공된 와이어프레임은 초기 레이아웃 참고에만 사용했고 원본을 공개·배포하지 않는다.
- 가상상점·가상문구점의 현황 사진 4장은 내장 image_gen으로 생성했다. 실제 촬영이 아니며 사진별 프롬프트·제작 정보는 각 `fixtures/.../photos/제작정보.md`에 있다. 외부 기관 사진을 복사한 것이 아니다.
- 신청서·가상 증빙·견적서는 직접 만든 테스트 자료다. 로고·관인·실제 개인정보를 사용하지 않는다. 문서 최상단·최하단의 안내만 빨간색으로 표시한다.
- PaddleOCR 3.3.3·PaddlePaddle 3.3.0과 공식 PP-OCRv5 모델을 실제 인식에 사용한다. PaddleOCR/PaddlePaddle은 Apache-2.0이며 모델별 배포 조건은 공식 모델 자료를 따른다. EasyOCR·Tesseract는 비교 후보로만 검토했다.
- FastAPI(MIT), Uvicorn(BSD-3-Clause), HTTPX(BSD-3-Clause), Pillow(MIT-CMU), NumPy(BSD-3-Clause)는 내부 OCR API에 사용한다. 의존성 고정값은 `ocr/requirements.txt`를 따른다.
- NVIDIA NIM·OpenRouter는 선택적 외부 API이며 모델을 자체 개발하거나 서비스에 재배포하지 않는다. 실제 검증 모델은 OpenRouter의 `google/gemma-4-26b-a4b-it`이며, NIM의 `google/gemma-4-31b-it` 호출은 시간 초과로 성공 처리하지 않았다. 이용 조건은 각 공급자와 선택 모델을 따른다.

임시 생성 파일은 `.gitignore`로 제외한다. 서비스 제공에 필요한 가상 PDF·사전 인식 산출물·원본 사진·출처 문서는 선택해 관리한다. 이전 작업물·외부 참고 프로젝트·로컬 도구·비밀 설정도 커밋 대상에서 제외한다. 파일별 출처 상세는 [.docs/references/SOURCE_PROVENANCE.md](.docs/references/SOURCE_PROVENANCE.md)를 따른다.

## 서비스 안내 일러스트 — 2026-10-25

메인 보조 안내 배너의 `src/main/webapp/assets/images/support-campaign-v1.png`는 기본 제공 이미지 생성 도구로 새로 제작했다. 실제 상점·수혜자·정부 지원 실적 사진이 아니다. [생성 프롬프트와 사용 범위](.docs/references/service-visuals/README.md)를 보존하며 화면에 AI 제작을 표시한다. 정부 사이트 조사 스크린샷의 사진·로고·캐릭터는 서비스 에셋으로 사용하지 않는다.

## 사업별 신청 예시·독립 서비스 브랜드 — 2026-10-25

새 사업 다섯 사례의 현황 예시 사진은 기본 제공 이미지 생성 도구로 제작했다. 실제 식당·꽃집·책방·공방·분식점의 촬영·시공·검사 자료가 아니다. 원본은 `fixtures/program-samples/*/현황예시.png`, 프롬프트와 제작 기록은 `fixtures/program-samples/사진제작기록.json`에 보존한다. 신청서·사업계획·견적·가상 사업자 정보는 자체 테스트 데이터이며 기관 증명서 원본을 배포하지 않는다.

마음이음 로고 시안과 추가 안내 이미지는 같은 이미지 생성 도구로 제작했다. 로고 기록은 `.docs/references/brand`, 거리 비주얼·투명 안내 아이콘 기록은 `.docs/references/service-visuals`를 따른다. 정부 상징·기관 로고로 사용하지 않는다. 선택한 A안만 공통 서비스 헤더에 적용했다.

사업별 개별 서식의 비정형 스캔 30장은 기본 제공 이미지 편집 도구의 img2img로 제작했다. 실제 촬영·스캔 자료가 아니며 글자·사진 세부를 재해석할 수 있다. 입력 PNG·전체 편집 프롬프트·출력 SHA256·검토 기록은 `.docs/references/document-scans/scan-generation-records.json`, 절차와 한계는 `.docs/25-개별서식및비정형스캔.md`에 있다. 정답은 원본 PDF와 JSON으로 보존한다. Apple Vision은 로컬 제작 검토 보조로만 사용했으며 서비스 OCR 공급자로 추가하지 않았다.

## 문서 처리 추가 의존성

- Apache PDFBox 3.0.8 / FontBox / pdfbox-io: Java PDF 모듈에서 원문 좌표·렌더링·이미지 영역·페이지 분할·스캔 사본 준비. Apache Software Foundation, Apache License 2.0. [공식 라이선스](https://github.com/apache/pdfbox/blob/trunk/LICENSE.txt). `pdf-engine/LICENSE`와 `pdf-engine/NOTICE`를 보존하고 실행 JAR의 `META-INF/LICENSE`·`META-INF/NOTICE`에 원본 고지를 포함한다. Jackson·Commons Logging 등 포함 의존성의 고지도 유지한다. 이전 PyMuPDF 실행 의존성은 PDFBox로 교체했다.
- OpenCV (`opencv-contrib-python` 4.10.0.84): Apache License 2.0. 스캔 페이지 내 사진 영역 후보 탐지. 포함 이미지와 후보 영역을 구분하고 원본에서 확인할 수 있게 한다.

## 404 안내 일러스트

`src/main/webapp/assets/images/not-found-neighborhood-v1.webp`는 이미지 생성 도구로 제작한 가상 상점 골목 그림이다. 기존 메인 히어로 `neighborhood-renewal-v1.webp`를 그림체 참고로 사용했으며 새 이미지에는 지도에서 길을 찾는 가게 주인들과 서류 위 고양이를 표현했다. 실제 인물·사업장·기관 사진이 아니다. 이미지의 문구는 HTML로 제공한다.

## 실행 이미지와 문서 제작 환경의 구분 — 2026-10-26

OCR 실행 이미지의 직접 의존성은 `ocr/requirements.txt`에 고정한다. Pillow 실행 버전은 13.0.0이며, 위 문서 제작 도구 표의 12.3.0과 별도 환경이다. PaddleOCR의 전이 의존성으로 설치되는 pypdfium2 등의 패키지도 자체 개발물로 표시하지 않는다. Java 실행 모듈의 의존성은 `pdf-engine/pom.xml`을 따른다. PyMuPDF·fitz를 사용하는 기존 제작 코드는 PDFBox로 교체했다. 오라클 실행 이미지에서도 두 모듈이 설치되지 않았고 실행 JAR에 LICENSE·NOTICE가 포함된 것을 확인했다.

오라클 linux/amd64 이미지에서 PDFBox 3.0.8, 실제 PaddleOCR, OpenRouter Gemma 4 호출을 검증했다. 상세 결과와 공개 서비스 반영 상태는 [PDFBox 전환 및 회귀 검증](.docs/33-PDFBox전환및회귀검증.md)에 기록한다. 공급자 모델의 이용 조건은 코드 라이선스와 별개다.

## KRDS 출처 표시

본 저작물은 행정안전부에서 2024년 작성하여 공공누리 제1유형으로 개방한 범정부 UI/UX 디자인시스템(KRDS)을 이용했다. 해당 자료는 [KRDS 공식 홈페이지](https://www.krds.go.kr/)에서 제공한다. [공식 저작권 및 라이선스 안내](https://www.krds.go.kr/html/site/utility/utility_06.html)를 따른다. 개별 폰트와 외부 플러그인은 위 별도 라이선스를 따른다.

제품의 CSS에서는 외부 이미지 URL을 로컬 상대 경로로 변경했다. eGovFrame·KRDS·정부기관이 이 서비스를 제작·인증·보증한다고 표시하지 않는다. 미사용 정부 상징 파비콘 4개와 태극기·KRDS 워드마크 SVG 3개는 공개 대상과 제품 자산에서 제거하고 해당 미사용 CSS 이미지 참조도 정리했다. 일반 KRDS UI 자산과 공식 정부 상징을 구분한다.

## 정부 사이트 조사 자료

정부 사이트 캡처·공식 목록 캡처·합성 갤러리 26개는 사용자 요청으로 현재 작업 파일에서 제거했다. 자체 관찰 기록과 원문 링크는 `.docs/references/government-sites/`에 보존하며 로컬 중복 사본도 정리했다. 이 고지가 캡처 속 제3자 사진·로고의 재배포 허락을 부여하지 않는다. 과거 Git 커밋의 제거 여부는 현재 파일 삭제와 별도로 확인해야 한다. 실제 기관 신청 서식 원본은 Git 제외 보관 경로에 있고 제품에 포함하지 않는다.

## Java 실행 의존성 목록

아래는 점검 당시 WAR의 라이브러리 60개를 기준으로 POM의 라이선스 상속 및 JAR 원문을 확인한 목록이다. JAR 자체는 Git에 포함하지 않는다. 바이너리 배포 시 원본의 LICENSE·NOTICE·소스 제공 조건과 예외를 보존해야 하며, 아래 표만으로 원문 고지를 대체하지 않는다. PDFBox 독립 모듈은 앞 절과 `pdf-engine/NOTICE`를 따른다.

| 라이브러리 파일 | 확인한 라이선스 |
| --- | --- |
| `aspectjrt-1.9.23.jar` | Eclipse Public License - v 2.0 |
| `aspectjweaver-1.9.23.jar` | Eclipse Public License - v 2.0 |
| `commons-beanutils-1.11.0.jar` | Apache-2.0 |
| `commons-codec-1.18.0.jar` | Apache-2.0 |
| `commons-collections-3.2.2.jar` | Apache License, Version 2.0 |
| `commons-collections4-4.4.jar` | Apache License, Version 2.0 |
| `commons-digester-2.1.jar` | The Apache Software License, Version 2.0 |
| `commons-io-2.18.0.jar` | Apache-2.0 |
| `commons-lang3-3.18.0.jar` | Apache-2.0 |
| `commons-logging-1.3.5.jar` | Apache-2.0 |
| `commons-validator-1.9.0.jar` | Apache-2.0 |
| `commons-vfs2-2.10.0.jar` | Apache-2.0 |
| `egovframe-rte-fdl-cmmn-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `egovframe-rte-fdl-filehandling-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `egovframe-rte-fdl-logging-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `egovframe-rte-fdl-string-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `egovframe-rte-psl-dataaccess-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `egovframe-rte-ptl-mvc-5.0.2.jar` | The Apache Software License, Version 2.0 |
| `flyway-core-11.14.1.jar` | Apache License, Version 2.0 |
| `flyway-mysql-11.14.1.jar` | Apache License, Version 2.0 |
| `HikariCP-6.3.3.jar` | The Apache Software License, Version 2.0 |
| `ibatis-sqlmap-2.3.4.726.jar` | Apache License 2.0 (JAR의 license.txt) |
| `jackson-annotations-2.19.2.jar` | The Apache Software License, Version 2.0 |
| `jackson-core-2.19.2.jar` | The Apache Software License, Version 2.0 |
| `jackson-databind-2.19.2.jar` | The Apache Software License, Version 2.0 |
| `jackson-datatype-jsr310-2.19.2.jar` | The Apache Software License, Version 2.0 |
| `jakarta.annotation-api-3.0.0.jar` | EPL 2.0 / GPL2 w/ CPE |
| `jakarta.el-api-5.0.0.jar` | Eclipse Public License v. 2.0 / GNU General Public License, version 2 with the GNU Classpath Exception |
| `jakarta.persistence-api-3.2.0.jar` | Eclipse Public License v. 2.0 / Eclipse Distribution License v. 1.0 |
| `jakarta.servlet-api-6.0.0.jar` | EPL 2.0 / GPL2 w/ CPE |
| `jakarta.servlet.jsp.jstl-3.0.1.jar` | EPL 2.0 / GPL2 w/ CPE |
| `jakarta.servlet.jsp.jstl-api-3.0.2.jar` | EPL 2.0 / GPL2 w/ CPE |
| `jakarta.validation-api-3.1.1.jar` | Apache License 2.0 |
| `jcl-over-slf4j-2.0.17.jar` | Apache-2.0 |
| `log4j-api-2.25.3.jar` | Apache-2.0 |
| `log4j-core-2.25.3.jar` | Apache-2.0 |
| `log4j-over-slf4j-2.0.17.jar` | Apache-2.0 |
| `log4j-slf4j2-impl-2.25.3.jar` | Apache-2.0 |
| `mariadb-java-client-3.5.6.jar` | LGPL-2.1-or-later |
| `micrometer-commons-1.14.11.jar` | The Apache Software License, Version 2.0 |
| `micrometer-observation-1.14.11.jar` | The Apache Software License, Version 2.0 |
| `mybatis-3.5.19.jar` | The Apache Software License, Version 2.0 |
| `mybatis-spring-3.0.5.jar` | The Apache Software License, Version 2.0 |
| `slf4j-api-2.0.17.jar` | MIT |
| `spring-aop-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-beans-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-context-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-context-support-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-core-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-expression-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-jcl-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-jdbc-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-orm-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-security-config-6.5.5.jar` | Apache License, Version 2.0 |
| `spring-security-core-6.5.5.jar` | Apache License, Version 2.0 |
| `spring-security-crypto-6.5.5.jar` | Apache License, Version 2.0 |
| `spring-security-web-6.5.5.jar` | Apache License, Version 2.0 |
| `spring-tx-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-web-6.2.11.jar` | Apache License, Version 2.0 |
| `spring-webmvc-6.2.11.jar` | Apache License, Version 2.0 |

MariaDB JDBC의 LGPL, Jakarta 계열의 EPL·GPL 및 Classpath Exception, AspectJ의 EPL 등은 각각의 조건을 따른다. 전부 Apache 라이선스라고 간주하지 않는다. 위 표에서 복수 라이선스·예외를 함께 표시한 경우 선택 또는 적용 범위는 해당 원문을 따른다.

## Python 실행 환경 의존성 목록

직접 의존성 고정값은 `ocr/requirements.txt`다. 아래는 점검한 로컬 OCR 환경의 설치 배포물 67개이며 서버의 전이 버전이 모두 같다는 의미는 아니다. Python 패키지는 Git에 복사하지 않으며 wheel·Docker 이미지를 배포할 때 각 배포물의 고지를 보존해야 한다. 일부 도구 전용 패키지도 포함한다.

| 배포물 | 버전 | 라이선스 표기 |
| --- | --- | --- |
| aistudio_sdk | 0.3.9 | Apache-2.0 |
| annotated-types | 0.8.0 | MIT |
| anyio | 4.15.1 | MIT |
| bce-python-sdk | 0.9.79 | Apache License 2.0 |
| certifi | 2026.10.19 | MPL-2.0 |
| cffi | 2.1.1 | MIT-0 |
| chardet | 7.7.0 | 0BSD |
| charset-normalizer | 3.5.2 | MIT |
| click | 8.5.0 | BSD-3-Clause |
| colorlog | 6.12.0 | MIT License |
| crc32c | 2.9.post0 | LGPL-2.1-or-later |
| cryptography | 50.0.2 | Apache-2.0 OR BSD-3-Clause |
| fastapi | 0.115.12 | MIT License |
| filelock | 4.2.4 | MIT |
| fsspec | 2026.9.0 | BSD-3-Clause |
| future | 1.0.0 | MIT |
| h11 | 0.16.0 | MIT |
| hf-xet | 1.7.0 | Apache-2.0 |
| httpcore | 1.0.9 | BSD-3-Clause |
| httpcore2 | 2.16.0 | BSD-3-Clause |
| httpx | 0.28.1 | BSD-3-Clause |
| httpx2 | 2.16.0 | BSD-3-Clause |
| huggingface_hub | 2.2.0 | Apache-2.0 |
| idna | 3.20 | BSD-3-Clause |
| imagesize | 2.0.1 | MIT |
| modelscope | 1.40.2 | Apache-2.0 |
| modelscope-hub | 0.4.6 | Apache-2.0 |
| networkx | 3.6.1 | BSD-3-Clause |
| numpy | 2.4.6 | BSD-3-Clause AND 0BSD AND MIT AND Zlib AND CC0-1.0 |
| opencv-contrib-python | 4.10.0.84 | Apache 2.0 |
| opt-einsum | 3.3.0 | MIT |
| packaging | 26.3 | Apache-2.0 OR BSD-2-Clause |
| paddleocr | 3.3.3 | Apache License 2.0 |
| paddlepaddle | 3.3.0 | Apache Software License |
| paddlex | 3.3.13 | Apache-2.0 |
| pandas | 3.1.0 | BSD-3-Clause |
| pillow | 13.0.0 | MIT-CMU |
| pip | 26.1.2 | MIT |
| prettytable | 3.18.0 | BSD-3-Clause |
| protobuf | 7.36.2 | 3-Clause BSD License |
| psutil | 7.2.2 | BSD-3-Clause |
| py-cpuinfo | 9.0.0 | MIT |
| pyclipper | 1.4.0 | MIT |
| pycparser | 3.12 | BSD-3-Clause |
| pycryptodome | 4.1.0 | BSD-2-Clause AND Unlicense AND MIT |
| pydantic | 2.14.1 | MIT |
| pydantic_core | 2.50.1 | MIT |
| pypdfium2 | 5.14.0 | BSD-3-Clause, Apache-2.0, dependency licenses |
| python-bidi | 0.6.12 | GNU Library or Lesser General Public License (LGPL) |
| python-dateutil | 3.0.0 | Dual License |
| PyYAML | 6.0.2 | MIT |
| requests | 2.34.2 | Apache-2.0 |
| ruamel.yaml | 0.19.1 | MIT |
| safetensors | 0.8.0 | Apache Software License |
| setuptools | 82.0.1 | MIT |
| shapely | 2.2.0 | BSD-3-Clause |
| six | 1.17.0 | MIT |
| starlette | 0.46.2 | BSD-3-Clause |
| tqdm | 4.70.1 | MPL-2.0 AND MIT |
| truststore | 0.10.5 | MIT |
| typing-inspection | 0.4.4 | MIT |
| typing_extensions | 4.16.0 | PSF-2.0 |
| tzdata | 2026.5 | Apache-2.0 |
| ujson | 6.0.0 | BSD-3-Clause AND TCL |
| urllib3 | 2.8.0 | MIT |
| uvicorn | 0.34.3 | BSD-3-Clause |
| wcwidth | 0.9.3 | MIT License |

`crc32c`·`python-bidi`의 LGPL 조건, `pypdfium2`의 BSD·Apache 및 내장 제3자 고지, OpenCV의 제3자 고지는 각 배포물에 포함된 원문을 따른다. Paddle 공식 모델 가중치의 배포 조건은 코드 라이선스와 별도로 확인한다. 이 프로젝트는 공식 모델 가중치를 Git에 재배포하지 않는다.

## 외부 서비스·운영 구성

NVIDIA NIM·OpenRouter·Oracle Cloud·Cloudflare는 사용하는 외부 서비스이며 이 프로젝트 자체 개발물이 아니다. 모델 가중치와 공급자 SDK를 복제해 판매하지 않는다. Gemma 모델·공급자 API의 이용 조건과 개인정보 처리는 오픈소스 코드 라이선스와 구분한다. Docker 기반 이미지와 Ubuntu 패키지·Caddy·Fail2ban은 원 배포물의 저작권·라이선스를 유지하며 실행 설정만 이 저장소에서 관리한다.

자체 작성 Java·SQL·JSP·JavaScript·CSS와 가상 자료의 사용 권한은 외부 라이브러리의 라이선스와 별개다. 루트에 자체 코드용 LICENSE가 없다는 사실이 제3자 저작권 고지를 무효로 하거나 자체 코드의 자유 재사용을 허락하는 것은 아니다.
