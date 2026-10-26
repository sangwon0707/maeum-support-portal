# 프레임워크 적용 및 SQL 설계

> **현재 구성 (2026-10-26):** eGovFrame 5 / Spring MVC / JSP / MyBatis Mapper XML / MariaDB 11.4 / Flyway V1–V12. OCR 문서 검토는 PDFBox 3.0.8과 PaddleOCR 연동, 선택적 OpenRouter/Gemma 4 경로를 사용한다. Oracle 실행 환경 확인은 Oracle DB 호환성 검증과 다르다. 외부 KRDS 출처는 [SOURCE_PROVENANCE.md](references/SOURCE_PROVENANCE.md)에 기록한다.

## 공식 배포 버전

실행환경은 공식 웹사이트의 5.0.2 배포와 대응하는 공식 GitHub `v5.0.2-Final` 태그를 사용한다. 최초 main 소스는 5.0.0이어서 버전을 재확인 후 교체했다. 원본은 수정하지 않고 공식 소스에서 필요한 모듈을 빌드했다. 공식 다운로드 ZIP의 바이너리와 바이트 단위로 동일하다는 주장은 하지 않는다.

공통컴포넌트 웹사이트 배포 버전 5.0.3은 별도 배포물이다. 이번 구현은 그 전체 업무 코드를 복사하지 않았고, eGovFrame 실행환경 위에 신청·검토·파일 업무를 작성했다. 공식 호환성 확인·인증을 받은 제품이라는 의미는 아니다.

| 공식 실행환경 모듈 | 실제 적용 |
|---|---|
| egovframe-rte-ptl-mvc 5.0.2 | 담당 업무 페이지의 PaginationInfo |
| egovframe-rte-psl-dataaccess 5.0.2 | EgovMapper, MapperConfigurer, MyBatis 연동 |
| egovframe-rte-fdl-cmmn 5.0.2 | 신청 업무 EgovAbstractServiceImpl, 메시지·추적 구성 |
| 실행환경 로깅 의존성 | SLF4J·Log4j2 로그 설정 |

KRDS 1.1.0의 common/component/token CSS와 글꼴·아이콘을 사용한다. 공개 출처 기록은 `.docs/references/SOURCE_PROVENANCE.md`에 둔다. `.ref`는 로컬 참고 원본이며 공개 저장소 기록에서 제외한다. 앱에는 필요한 KRDS CSS를 복사하고 리소스 주소를 로컬 상대 경로로 조정했다. 화면에는 공식 버튼·입력·선택 컴포넌트 클래스를 사용하고 레이아웃은 app.css로 분리했다. 최종 인증·전체 접근성 준수 판정을 뜻하지 않는다.

## Spring Boot·JPA 선택

이번 구현은 Spring Boot 없이 Spring MVC·Security·MyBatis를 Java 설정으로 조립한다. 내장 Tomcat을 로컬 실행기에 사용하지만 Boot 앱은 아니다. 같은 WAR를 외부 Linux Tomcat에 배포한다. eGovFrame과 Boot는 함께 사용할 수 있고 eGovFrame에서도 JPA를 사용할 수 있다.

데이터 접근은 JPA/ORM 대신 MyBatis SQL 매핑을 선택했다. 조건 검색·집계·행 잠금 SQL을 직접 관리하고 트랜잭션과 실행계획을 검증하기 위한 선택이다. SQL 자동 생성을 제공하지 않으므로 Mapper와 Flyway를 변경하고 통합 테스트로 검증해야 한다.

## 조회 SQL

ApplicationMapper.xml: 신청 탐색·검색·전체 수·배정 검사·행 잠금·버전 갱신·불변 제출본·중복 제출 키.
ReportingMapper.xml: 상태별 집계·담당자 집계·보완 기한 초과.
DocumentMapper.xml: 종류별 최신 버전과 원본 문서.
OcrMapper.xml: 대기/lease 만료 claim·결과 멱등 저장·확인값 분리.

조회 조건은 `<sql id="filter">`로 목록과 count에 공유한다. 검색 값은 `#{...}`로 바인딩하며 `${...}` SQL 삽입은 사용하지 않는다. 담당 범위는 EXISTS로 검사해 여러 서류·이력 행 때문에 신청 수가 부풀지 않게 한다. 정렬은 updated_at DESC,id DESC로 동률을 고정한다.

## 변경·트랜잭션

Service의 @Transactional 안에서 신청 행을 FOR UPDATE로 잠그고 expectedVersion을 비교한다. 상태 변경·이력·제출 스냅샷·그 당시의 첨부 연결을 함께 저장한다. 같은 제출 키의 재요청은 이전 revision을 반환한다. 키 재사용에 다른 내용이 들어오면 충돌이다.

현재 첨부와 제출본 첨부는 구분한다. 교체는 새 document 버전이며 기존 파일을 덮어쓰지 않는다. 파일 저장 뒤 DB 롤백이 발생하면 이번 파일만 보상 삭제한다. OCR 결과와 사용자가 확인한 값도 별도 테이블에 보관한다.

## 인덱스

초기 설계 인덱스에 더해 V2에서 실제 정렬에 맞는 (workspace_id,updated_at,id), (workspace_id,status,updated_at,id)를 추가했다. 측정에서는 후보 인덱스를 IGNORE INDEX로 제외한 경우와 사용한 경우의 결과 정합성·EXPLAIN을 비교한다. 운영 전후 관측을 조작해 만든 수치가 아니라 로컬 비교 실험이다.

스키마와 기본 키/제약은 V1, 조회 인덱스는 V2로 구분한다. 적용한 마이그레이션을 임의로 수정하지 않고 새 버전으로 변경한다. 실제 DDL은 src/main/resources/db/migration에 있다.

## MVC 패키지와 SQL 매핑

업무 이름별 혼합 패키지를 `controller`, `service`, `mapper`, `dto`, `model` 중심으로 정리했다. MyBatis XML namespace와 resultType은 새 인터페이스·모델 패키지로 갱신했고 SQL 문장·테이블·컬럼·마이그레이션은 유지했다. MapperConfigurer는 mapper 패키지만 스캔한다. 요청 경로·서비스 메서드의 업무 규칙과 트랜잭션 경계는 바꾸지 않았다.
