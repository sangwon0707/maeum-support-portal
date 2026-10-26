-- 실제 정렬(updated_at, id)과 맞춘 인덱스다. 최초 설계의 submitted_at 인덱스와 용도가 다르다.
CREATE INDEX ix_application_sort ON application(workspace_id, updated_at, id);
CREATE INDEX ix_application_status_sort ON application(workspace_id, status, updated_at, id);
