-- 게임 단위 GOL.GG Blue/Red 사이드 팀명.
-- 사이드 순서가 로컬 match.teamA/teamB와 다른 경우 UI 통계 swap 버그를 막기 위함.
-- 파싱 실패 시 NULL — 추측 보간 금지(Hard Rule #4). 프론트는 NULL을 "-"로 표시.
ALTER TABLE match_external_detail_game
    ADD COLUMN IF NOT EXISTS blue_team_name varchar(100),
    ADD COLUMN IF NOT EXISTS red_team_name varchar(100);
