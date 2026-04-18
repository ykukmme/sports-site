ALTER TABLE teams RENAME COLUMN region TO league;

UPDATE teams
SET league = CASE
    WHEN league IN ('한국', '대한민국', 'Korea', 'South Korea') THEN 'LCK'
    WHEN league IN ('중국', 'China') THEN 'LPL'
    WHEN league IN ('유럽', 'Europe', 'EU') THEN 'LEC'
    WHEN league IN ('북미', 'North America', 'NA') THEN 'LCS'
    WHEN league IN ('브라질', 'Brazil') THEN 'CBLOL'
    WHEN league IN ('태평양', 'Pacific') THEN 'LCP'
    ELSE league
END;
