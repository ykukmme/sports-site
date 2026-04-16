-- 기본 종목 시드 데이터
-- 어드민에서 팀/경기를 등록하려면 최소 1개 이상의 종목이 필요하다.

INSERT INTO games (name, short_name)
VALUES
    ('League of Legends', 'LoL'),
    ('Valorant', 'VAL'),
    ('Overwatch 2', 'OW2')
ON CONFLICT (name) DO NOTHING;
