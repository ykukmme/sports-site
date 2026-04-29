ALTER TABLE match_external_detail_game
    ADD COLUMN blue_towers integer,
    ADD COLUMN red_towers integer,
    ADD COLUMN blue_team_gold integer,
    ADD COLUMN red_team_gold integer,
    ADD COLUMN first_blood_side varchar(10),
    ADD COLUMN first_tower_side varchar(10),
    ADD COLUMN blue_dragon_types_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN red_dragon_types_json jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE match_external_detail_game
    ADD CONSTRAINT chk_match_external_detail_game_first_blood_side
        CHECK (first_blood_side IS NULL OR first_blood_side IN ('BLUE', 'RED')),
    ADD CONSTRAINT chk_match_external_detail_game_first_tower_side
        CHECK (first_tower_side IS NULL OR first_tower_side IN ('BLUE', 'RED'));
