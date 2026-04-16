# Session Handoff: Player List Fix

Date: 2026-04-16

## Current Issue

The admin player create flow returned to `/admin/players`, but the newly created player was not visible.

## Root Cause

Player creation was succeeding, but `AdminPlayerListPage` built the player table from `/api/v1/teams` by reading `team.players`.

The public team list endpoint returns team summaries and does not include the `players` array. Only the team detail endpoint includes players. Because of that, the admin player table could stay empty even after a successful create.

## Fix Applied

- Added backend public player list endpoint:
  - `GET /api/v1/players`
  - `backend/src/main/java/com/esports/domain/player/PlayerController.java`
  - `backend/src/main/java/com/esports/domain/player/PlayerQueryService.java`
- Added frontend admin player list API/hook:
  - `fetchAdminPlayers`
  - `useAdminPlayerList`
  - `frontend/src/api/admin.ts`
  - `frontend/src/hooks/useAdminPlayers.ts`
- Updated admin player list page:
  - Reads players directly from `/api/v1/players`.
  - Uses `/api/v1/teams` only to map `teamId` to team name.
  - Shows free agents as `미소속`.
  - `frontend/src/pages/admin/players/AdminPlayerListPage.tsx`
- Added controller test:
  - `PlayerControllerTest.listReturns200`

## Verification

- `npm.cmd run build`: passed.
- `.\gradlew.bat test`: passed.
- `git diff --check`: passed.

## Deployment Note

Latest local frontend bundle after this fix:

- `index-ndvWx_Mc.js`

Production needs both frontend and backend rebuilt/redeployed, because the frontend now calls the new backend endpoint `GET /api/v1/players`.
