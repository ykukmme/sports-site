# Session Handoff

## Current Status
1. Gol.gg enrichment Phase 1 implemented (local):
   - Flyway V11 added:
     - `match_external_detail`
     - `match_external_detail_game`
   - Admin APIs added:
     - `POST /api/admin/matches/{id}/details/bind`
     - `POST /api/admin/matches/{id}/details/sync`
     - `POST /api/admin/matches/details/sync`
   - Public/API read model extended:
     - `GET /api/v1/matches/{id}` now includes optional `detailSummary`
   - Gol.gg client/service added:
     - `GolGgClient`
     - `GolDetailEnrichmentService`
   - Config added:
     - `golgg.*` in `application.yml`
     - env example keys (`GOLGG_*`)
2. Tests passed locally:
   - `AdminMatchControllerTest`
   - `MatchControllerTest`
   - `GolDetailEnrichmentServiceTest`

## Next Actions (priority order)
1. Deploy to EC2 and run migration:
   - `git pull origin master`
   - `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`
2. Production API smoke checks:
   - bind URL: `POST /api/admin/matches/{id}/details/bind`
   - sync single: `POST /api/admin/matches/{id}/details/sync`
   - sync batch: `POST /api/admin/matches/details/sync`
   - read: `GET /api/v1/matches/{id}` includes `detailSummary`
3. Phase 1 stabilization:
   - verify failure isolation (`FAILED` should not block match/result flow)
   - verify `NEEDS_REVIEW` branch on multiple game-id detection
4. Prepare Phase 2:
   - candidate auto-matching by schedule/team/tournament
   - admin review UI for ambiguity resolution

## Open Decisions
- Keep storing full page HTML-derived payload in `raw_json` as-is vs minimal subset only.
- Define whether batch sync with empty `matchIds` should mean "all bound only" or "all details row".

## Remaining Issues
- No blocker in local test run.
- Existing mojibake comments/notes in legacy files are still present (non-runtime).

## Current Focus
- Deploy and verify Gol.gg Phase 1 in production, then move to Phase 2 matching heuristics.
