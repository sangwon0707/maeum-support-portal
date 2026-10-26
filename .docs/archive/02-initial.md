# 업무 흐름도 / Flowchart

> 보존 기록: 본문의 예정·미구현·검토 대기·배포 상태는 작성 당시의 기록이다. 현재 기능과 운영 상태는 원래 번호 문서 및 15·31~33번을 따른다.


> 초기 설계의 범위·수용 기준을 보존한 문서다. 현재 코드와 검증 이력은 [구현 검증 결과](../15-구현검증결과.md), 새 예시 서류와 미연동 범위는 [예시 자료](../17-예시신청서및검증자료.md)를 따른다. 모든 수용 기준이 완료됐다는 뜻은 아니다.

설계 기준 0.1 · 현황 갱신 2026-10-23

## 1. 전체 업무 흐름

```mermaid
flowchart TD
    A[SC-01 공고 탐색] --> B[SC-02 공고 상세]
    B --> C[SC-03 로그인]
    C --> D[SC-04 신청 임시저장]
    D --> E[SC-05 필수 서류 첨부]
    E --> F{OCR 사용?}
    F -->|사용| G[SC-06 추출값 대조·확인]
    F -->|생략·실패| H[수동 확인 입력]
    G --> I[SC-07 최종 확인]
    H --> I
    I --> J{기간·필수 항목·버전 유효?}
    J -->|오류| D
    J -->|정상| K[SC-08 제출 완료·진행 조회]
    K --> L[SC-13 관리자 배정]
    L --> M[SC-10 담당 업무 목록]
    M --> N[SC-11 검토 시작]
    N --> O{서류 보완 필요?}
    O -->|예| P[항목·사유·기한 지정]
    P --> Q[SC-09 보완]
    Q --> R{보완 기한·변경 범위 유효?}
    R -->|아니오| Q
    R -->|예| K
    O -->|아니오| S[선정·미선정 사유 확정]
    S --> T[SC-08 결과 조회]
```

## 2. 상태 전이와 책임

```mermaid
stateDiagram-v2
    [*] --> DRAFT: 신청자 작성
    DRAFT --> SUBMITTED: 최초 제출
    SUBMITTED --> UNDER_REVIEW: 배정 담당자 검토 시작
    UNDER_REVIEW --> NEEDS_SUPPLEMENT: 담당자 보완 요청
    NEEDS_SUPPLEMENT --> SUBMITTED: 신청자 재제출
    UNDER_REVIEW --> APPROVED: 담당자 선정
    UNDER_REVIEW --> REJECTED: 담당자 미선정
```

보완 요청의 항목이 모두 응답되고 기한 이내일 때만 재제출한다. 상태 전이는 버전 비교·업무 데이터·상태 이력을 한 트랜잭션으로 저장한다. 확정 후 취소·철회·재개방은 범위에 포함하지 않는다.

## 3. OCR 비동기 흐름

```mermaid
sequenceDiagram
    actor U as 신청자
    participant A as 업무 서버
    participant F as 비공개 파일
    participant D as MariaDB
    participant W as 작업 실행기
    participant O as OcrProvider
    U->>A: 가상 서류 등록
    A->>F: 검증 후 저장
    A->>D: 문서·작업 저장
    A-->>U: 작업 ID 반환
    W->>D: 원자적 작업 claim·lease 설정
    W->>F: 해당 문서 버전 읽기
    W->>O: SAMPLE 또는 LIVE 호출
    alt 정상 응답
        O-->>W: 추출값
        W->>D: 결과 멱등 저장·완료
    else 일시 오류
        W->>D: 제한 재시도 예약 또는 실패
    end
    U->>A: 상태 조회
    A->>D: 권한·문서 버전 확인
    A-->>U: 대조 결과 또는 수동 입력 안내
```

오래 걸리는 공급자 호출은 DB 트랜잭션 밖에서 수행한다. lease 회수로 중복 실행 가능하므로 완료 갱신에는 작업 토큰을 비교하고 결과는 job당 1개로 제한한다.

## 4. 데모 흐름

```mermaid
flowchart LR
    A[SC-00 시나리오 선택] --> B[세션 귀속 공간 생성]
    B --> C[가상 신청서·서류·샘플 OCR 복제]
    C --> D[신청자 화면 체험]
    D --> E[같은 공간 담당자 화면 전환]
    E --> F[보완·결과 처리]
    F --> G[본인 공간만 재시작 또는 만료 정리]
```

SC-00의 데이터 채우기와 HTML 와이어프레임의 화면 전환은 다르다. 와이어프레임은 DB·인증·OCR을 실행하지 않는다.
