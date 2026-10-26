-- 일반 계정은 기존 1회 신청을 유지하고 테스트 계정의 반복 실행만 별도 회차로 보존한다.
ALTER TABLE application
  ADD COLUMN application_round INT NOT NULL DEFAULT 1,
  DROP INDEX workspace_id,
  ADD UNIQUE KEY uq_application_round (workspace_id, program_id, owner_id, application_round);
