-- 원문·인식 모델·항목 근거를 신청값·사용자 확인값과 분리한다.
ALTER TABLE ocr_result ADD COLUMN evidence JSON, ADD COLUMN raw_result JSON;
-- 날짜·공간별 예약 카운터를 잠가 동시 요청도 외부 AI 사용 한도를 넘지 않게 한다.
CREATE TABLE ai_daily_usage (
  usage_day DATE NOT NULL,
  scope_key VARCHAR(50) NOT NULL,
  requests INT NOT NULL DEFAULT 0,
  PRIMARY KEY (usage_day, scope_key)
);
