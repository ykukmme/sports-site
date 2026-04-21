# Next Features (Prioritized)

## P1 - Gol.gg Enrichment
1. Phase 1 (done in code, needs production verification)
   - schema:
     - `match_external_detail`
     - `match_external_detail_game`
   - admin API:
     - `POST /api/admin/matches/{id}/details/bind`
     - `POST /api/admin/matches/{id}/details/sync`
     - `POST /api/admin/matches/details/sync`
   - read API:
     - `GET /api/v1/matches/{id}` optional `detailSummary`
2. Phase 1 stabilization (next)
   - production smoke + failure-mode validation
   - `NEEDS_REVIEW` and `FAILED` operational checks
3. Phase 2 (next implementation)
   - auto candidate matching by schedule/team/tournament
   - manual resolve flow for ambiguous candidates

## P2 - Admin Productivity
1. Batch actions:
   - bulk delete
   - bulk status update
   - bulk re-sync selected external IDs
2. Saved filter presets:
   - league + team + sinceDate + sort
3. Match detail drawer:
   - external metadata
   - sync history
   - result history

## P3 - Public UX
1. Unified filter bar behavior parity (upcoming/results)
2. Show enriched detail card only when data exists
3. Large result-set pagination/virtualization tuning

## P4 - Operations/Automation
1. Scheduler phased rollout (dry-run -> alert-only -> apply)
2. Nightly sync report channel integration
3. Rollback playbook and kill-switch flags

## Done (Do Not Reopen)
- International competition baseline and filtering:
  - FIRST STAND
  - MSI
  - WORLDS
- Admin/user date sort filters
- PandaScore completed sync hardening (batch lookup + page limit config)
