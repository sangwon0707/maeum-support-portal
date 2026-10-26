-- 공고 이름을 수정해도 개발용 가상 공고를 다시 만들거나 덮어쓰지 않도록 식별한다.
ALTER TABLE program ADD COLUMN seed_key VARCHAR(80) NULL UNIQUE;
