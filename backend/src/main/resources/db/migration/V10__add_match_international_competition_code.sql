ALTER TABLE matches
    ADD COLUMN international_competition_code VARCHAR(50);

CREATE INDEX idx_matches_international_competition_code
    ON matches(international_competition_code);

UPDATE matches
SET international_competition_code = 'INTERNATIONAL_FIRST_STAND'
WHERE international_competition_code IS NULL
  AND (
    lower(coalesce(tournament_name, '')) LIKE '%first stand%'
    OR lower(coalesce(stage, '')) LIKE '%first stand%'
  );

UPDATE matches
SET international_competition_code = 'INTERNATIONAL_MSI'
WHERE international_competition_code IS NULL
  AND (
    lower(coalesce(tournament_name, '')) LIKE '%mid season invitational%'
    OR lower(coalesce(tournament_name, '')) LIKE '%msi%'
    OR lower(coalesce(stage, '')) LIKE '%mid season invitational%'
    OR lower(coalesce(stage, '')) LIKE '%msi%'
  );

UPDATE matches
SET international_competition_code = 'INTERNATIONAL_WORLDS'
WHERE international_competition_code IS NULL
  AND (
    lower(coalesce(tournament_name, '')) LIKE '%league of legends world championship%'
    OR lower(coalesce(tournament_name, '')) LIKE '%world championship%'
    OR lower(coalesce(tournament_name, '')) LIKE '%worlds%'
    OR lower(coalesce(stage, '')) LIKE '%league of legends world championship%'
    OR lower(coalesce(stage, '')) LIKE '%world championship%'
    OR lower(coalesce(stage, '')) LIKE '%worlds%'
  );

UPDATE matches
SET stage = CASE international_competition_code
    WHEN 'INTERNATIONAL_FIRST_STAND' THEN 'FIRST STAND'
    WHEN 'INTERNATIONAL_MSI' THEN 'MSI'
    WHEN 'INTERNATIONAL_WORLDS' THEN 'WORLDS'
    ELSE stage
END
WHERE international_competition_code IS NOT NULL
  AND (
    stage IS NULL
    OR lower(stage) = 'international'
    OR stage = '국제전'
  );
