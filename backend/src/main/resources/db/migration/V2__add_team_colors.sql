-- 팀 색상 컬럼 추가 — 팬 테마 시스템용 hex 코드 저장 (#RRGGBB)
ALTER TABLE teams
    ADD COLUMN primary_color   VARCHAR(7),
    ADD COLUMN secondary_color VARCHAR(7);
