# 서비스 안내 일러스트

- 생성일: 2026-10-25
- 도구: 기본 제공 이미지 생성 도구. 외부 CLI·사용자 API 키는 사용하지 않았다.
- 유형: illustration-story
- 파일: [support-campaign-v1.png](../../../src/main/webapp/assets/images/support-campaign-v1.png), 1536×1024 PNG
- 용도: 메인 공고 소개의 보조 장식. 모바일에서는 읽기 공간 확보를 위해 숨긴다. 서비스 내 AI 제작 안내를 함께 표시한다.
- 실제 지원 상점·수혜자·정부기관 사진이 아니며 실제 지원 실적을 뜻하지 않는다.
- 정부 사이트 레퍼런스의 이미지·로고·캐릭터를 입력하거나 복제하지 않았다.

## 생성 프롬프트

Use case: illustration-story. Asset type: original editorial banner illustration for a Korean small-business support application portal. Create one polished wide landscape illustration, 3:2 composition, depicting a compact neighborhood storefront with a striped cobalt-blue awning, warm window light, one small potted tree, and an oversized neatly stacked application document with a subtle blue check symbol beside the storefront. Refined contemporary public-service campaign illustration, dimensional paper-cut and lightly textured matte 3D forms, realistic proportions, simple readable silhouette, professional not childish. Soft off-white and pale sky-blue backdrop, cobalt and navy accents, restrained warm apricot details. Center scene with generous pale negative space around it; all elements comfortably within frame for responsive crop. No people, no readable text, no letters or numerals, no logos, no national insignia, no watermark. This is a decorative conceptual service illustration, not a photograph or evidence of a real funded business.

## 실제 서비스 연결 — 2026-10-25

별도 시안의 `neighborhood-renewal-v1`과 안내 아이콘 4개를 실제 메인에도 연결했다. 사업별 현황 사진 5개는 `fixtures/program-samples/*/현황예시.png`와 `사진제작기록.json`을 원본·프롬프트로 사용한다. `program-*.webp`는 해당 PNG를 `cwebp` 품질 82로 포맷 변환한 웹 전송본이다. 변환 스크립트는 `scripts/prepare-portal-images.py`이며 생성 PNG를 대체하지 않는다. 실제 지원 업체·실적 사진을 뜻하지 않는다.
