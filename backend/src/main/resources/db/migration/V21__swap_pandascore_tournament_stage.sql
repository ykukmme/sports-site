-- PandaScore 출처 매치의 tournament_name 과 stage 필드 의미가 뒤바뀌어 저장되어 있던 것을 스왑한다.
-- 이전: tournament_name = tournament.name (단계), stage = league.name (리그)
-- 이후: tournament_name = league.name (리그), stage = tournament.name (단계)
-- stage 가 NULL 인 row 는 백필 대상에서 제외 (NOT NULL 인 tournament_name 을 NULL 로 만들 수 없음).
-- 해당 row 는 다음 sync 때 자연 정정된다.
UPDATE matches
SET tournament_name = stage,
    stage = tournament_name
WHERE external_source = 'PANDASCORE'
  AND stage IS NOT NULL;
