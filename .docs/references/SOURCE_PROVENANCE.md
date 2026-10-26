# 공식 소스와 사용 범위

확인일: 2026-10-23

- eGovFrame 공식 웹사이트 실행환경 5.0.2: https://egovframe.go.kr/home/sub.do?menuNo=106
- 공식 런타임: https://github.com/eGovFramework/egovframe-runtime
- 고정 태그: v5.0.2-Final
- 커밋: 8f47a567ec4a8d3969f423c195115648cf562ae0
- 보존 경로: .ref/egovframe/runtime (원본 소스 변경 없음). 필요한 모듈과 선행 의존성을 Maven으로 빌드했다.
- 원본 템플릿 POM은 출처 검토용 .ref/egovframe/official-template-pom.xml에 보존했으며 서비스 템플릿으로 복제하지 않았다.
- 공식 공통컴포넌트 5.0.3은 별도 배포물이며 이번 서비스에 전체 복사하지 않았다.

KRDS 공식 사이트의 HTML Component Kit 1.1.0 링크를 따라 공식 저장소를 확보했다.
- https://www.krds.go.kr/html/site/outline/outline_05.html
- https://github.com/KRDS-uiux/krds-uiux
- 태그 1.1.0, 커밋 d6bb184c823e4757f05807ea4646a23e3133b6e6
- 보존 경로 .ref/krds/source. 제품에는 resources의 CSS·글꼴·아이콘만 사용하고 참고 사이트/예시 HTML은 복사하지 않았다.
- 제품용 CSS의 외부 이미지 URL을 로컬 상대 경로로 변경했다. 원본은 보존했다.

가상 문서 글꼴: Google Fonts의 Noto Sans KR, SIL Open Font License.
- https://github.com/google/fonts/tree/main/ofl/notosanskr
- 제품 경로 src/main/resources/fonts. OFL.txt 동봉.

제공된 와이어프레임은 초기 레이아웃 참고에만 사용했다. 원본은 공개 저장소·배포 WAR에 포함하지 않는다.

## 문서·사진 참고 자료 갱신

- 실제 기관 서식: 수원시 2026 신청·완료 서식, 전북 2026 신청 서식(HWP와 제공된 PDF), 충주시 2024 서식(HWP와 민간 보관 PDF). 현재 위치는 `.archive/예시문서-이전버전/참고서식/원본및출처/`. 해당 폴더의 출처 문서·JSON에 URL·해시·확인 범위가 있다. 비교 기록은 같은 보관 폴더에 있다. 민간 PDF와 공식 HWP가 동일한 배포 파일이라고 단정하지 않는다.
- 위 기관 자료는 서식 구조의 참고용이며 배포 WAR의 서비스 자료로 복사하지 않는다.
- AI 현황 사진: 가상상점 2장(`fixtures/support-application-v2/photos/`), 가상문구점 2장(`fixtures/support-application-comparison/photos/`). 내장 image_gen 생성이며 실제 촬영이 아니다. 각 제작정보.md에 프롬프트를 기록했다.
- 문서 생성 라이브러리와 라이선스 표기는 루트 THIRD_PARTY_NOTICES.md를 따른다. PaddleOCR 실제 연동과 외부 AI 선택 기능은 19번 보고서에 기록한다.

일반 프로젝트 진입 파일은 `README.md`를 사용한다. 생성 산출물·로컬 도구·이전 보관본은 Git에 포함하지 않는다.

## 공개 저장소와 로컬 원본의 구분

`.ref/`는 로컬 보존 전용이며 Git·WAR에 포함하지 않는다. 공개에 필요한 자체 출처·생성 기록은 이 디렉터리에 별도로 정리했다. 정부 사이트 캡처와 실제 기관 양식 원본은 공개하지 않는다.

## 참고 사본 정리

브랜드·서비스 이미지 생성 기록과 정부 사이트 관찰 기록은 이 디렉터리의 사본으로 관리하며 `.ref/`의 중복 사본을 정리했다. 이용정책 참고 웹페이지 원문도 삭제했다. `.ref/`에는 공식 소스와 재생성 스크립트가 사용하는 스캔 제작 기록만 남긴다.
