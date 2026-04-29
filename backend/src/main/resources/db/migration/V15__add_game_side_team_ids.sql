ALTER TABLE match_external_detail_game
    ADD COLUMN blue_team_id BIGINT,
    ADD COLUMN red_team_id BIGINT;

ALTER TABLE match_external_detail_game
    ADD CONSTRAINT fk_match_external_detail_game_blue_team
        FOREIGN KEY (blue_team_id) REFERENCES teams(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_match_external_detail_game_red_team
        FOREIGN KEY (red_team_id) REFERENCES teams(id) ON DELETE SET NULL;

CREATE INDEX idx_match_external_detail_game_blue_team_id
    ON match_external_detail_game(blue_team_id);

CREATE INDEX idx_match_external_detail_game_red_team_id
    ON match_external_detail_game(red_team_id);
