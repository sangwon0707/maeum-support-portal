-- 공고의 판단 정보와 신청자의 사업장·사업비 정보를 업무 데이터로 관리한다.
ALTER TABLE program
  ADD COLUMN target_text TEXT,
  ADD COLUMN support_text TEXT,
  ADD COLUMN exclusion_text TEXT,
  ADD COLUMN selection_text TEXT,
  ADD COLUMN contact_text VARCHAR(500);
ALTER TABLE application
  ADD COLUMN address VARCHAR(300),
  ADD COLUMN business_type VARCHAR(100),
  ADD COLUMN business_item VARCHAR(100),
  ADD COLUMN self_contribution DECIMAL(12,0);
UPDATE program SET
  target_text='공고 접수 기간에 영업 중이며 매장 환경개선 계획을 가진 소상공인. 사업장 소재지와 사업자등록증의 기재 내용을 확인합니다.',
  support_text='노후 간판 교체, 매장 조명 개선, 전기 배선 정비 등 매장 이용 환경 개선 비용. 신청금액은 공고의 지원 한도 이내로 작성하고 자부담은 별도로 기재합니다.',
  exclusion_text='사업과 관련 없는 물품 구입, 이미 완료한 공사, 증빙할 수 없는 지출은 지원 검토에서 제외합니다. 동일 비용에 대한 중복 지원 여부는 담당자가 확인합니다.',
  selection_text='필수 서류 구비 여부, 신청 정보와 증빙의 일치, 개선 계획의 구체성 및 견적의 적정성을 검토합니다. 누락·불일치는 보완 요청 후 재검토합니다.',
  contact_text='마음이음 사업운영팀 · 문의 방법과 연락처는 실제 운영기관 설정 후 안내합니다.'
WHERE title='2026 소상공인 매장 환경개선 지원사업';
