-- 게임 단위 부분 실패 메시지 컬럼.
-- T-1.5 enrichment에서 한 게임 fetch 실패 시 다른 게임은 정상 저장하고
-- 실패 게임에만 사람이 읽을 수 있는 에러 메시지를 남기기 위함.
-- NULL은 정상(에러 없음). 빈 문자열은 사용하지 않는다.
ALTER TABLE match_external_detail_game
    ADD COLUMN IF NOT EXISTS error_message text;
