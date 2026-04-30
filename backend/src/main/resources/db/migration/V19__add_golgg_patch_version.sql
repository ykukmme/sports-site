ALTER TABLE gol_gg_indexed_match
    ADD COLUMN patch_version VARCHAR(20);

ALTER TABLE match_external_detail
    ADD COLUMN patch_version VARCHAR(20);
