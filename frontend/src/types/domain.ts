export interface AiSummaryResponse {
  matchId: number
  summaryText: string
  modelVersion: string
  generatedAt: string
}

export interface ChatbotResponse {
  answer: string
}

export interface GameResponse {
  id: number
  name: string
  shortName: string
}

export interface MatchResultResponse {
  scoreTeamA: number
  scoreTeamB: number
  winnerTeamId: number | null
  playedAt: string
  vodUrl: string | null
}

export interface TeamSummary {
  id: number
  name: string
  shortName: string
}

export type MatchStatus = 'SCHEDULED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'
export type MatchExternalSource = 'MANUAL' | 'PANDASCORE'
export type PlayerStatus = 'ACTIVE' | 'INACTIVE' | 'RETIRED'
export type PlayerExternalSource = 'MANUAL' | 'PANDASCORE'
export type MatchExternalDetailStatus = 'PENDING' | 'SYNCED' | 'PARTIAL_SYNC' | 'FAILED' | 'NEEDS_REVIEW'

export interface MatchExternalDetailSummaryResponse {
  provider: string | null
  status: MatchExternalDetailStatus | null
  sourceUrl: string | null
  confidence: number | null
  expectedGameCount: number | null
  syncedGameCount: number | null
  validationStatus: string | null
  validationMessage: string | null
  teamMatchConfidence: number | null
  dateMatchConfidence: number | null
  lastSyncedAt: string | null
  errorMessage: string | null
  boundBy: 'AUTO' | 'MANUAL' | null
  boundAt: string | null
  boundScore: number | null
}

export interface MatchExternalDetailPublicPick {
  championId: string | null
  playerName: string | null
  position: string | null
  kills: number | null
  deaths: number | null
  assists: number | null
  cs: number | null
  summonerSpells: string[]
  items: string[]
}

export interface MatchExternalDetailPublicObjectiveEvent {
  timeSec: number | null
  side: string | null
  type: string | null
  label: string | null
}

export interface MatchExternalDetailPublicDistributionEntry {
  side: string | null
  position: string | null
  percent: number | null
  perMinute: number | null
}

export interface MatchExternalDetailPublicGoldTimelinePoint {
  timeSec: number | null
  blueGold: number | null
  redGold: number | null
  goldDiff: number | null
}

export interface MatchExternalDetailPublicGame {
  gameNo: number | null
  durationSec: number | null
  winnerSide: string | null
  blueTeamName: string | null
  redTeamName: string | null
  blueTeamId: number | null
  redTeamId: number | null
  blueKills: number | null
  redKills: number | null
  blueDragons: number | null
  redDragons: number | null
  blueBarons: number | null
  redBarons: number | null
  blueTowers: number | null
  redTowers: number | null
  blueTeamGold: number | null
  redTeamGold: number | null
  firstBloodSide: string | null
  firstTowerSide: string | null
  blueDragonTypes: string[]
  redDragonTypes: string[]
  blueBans: string[]
  redBans: string[]
  bluePicks: MatchExternalDetailPublicPick[]
  redPicks: MatchExternalDetailPublicPick[]
  objectiveTimeline: MatchExternalDetailPublicObjectiveEvent[]
  bluePlates: number | null
  redPlates: number | null
  goldTimeline: MatchExternalDetailPublicGoldTimelinePoint[]
  goldDistribution: MatchExternalDetailPublicDistributionEntry[]
  damageDistribution: MatchExternalDetailPublicDistributionEntry[]
  errorMessage: string | null
}

// 게임 보정(override) 관련 타입 — Admin 보정 폼/API 용
export interface GameOverrideIntegerField {
  base: number | null
  override: number | null
  effective: number | null
}

export interface GameOverrideView {
  gameNo: number | null
  durationSec: GameOverrideIntegerField
  blueTeamGold: GameOverrideIntegerField
  redTeamGold: GameOverrideIntegerField
  goldDistributionOverridden: boolean
  damageDistributionOverridden: boolean
  goldDistributionOverrides: GameOverrideDistributionEntry[]
  damageDistributionOverrides: GameOverrideDistributionEntry[]
  note: string | null
  updatedBy: string | null
  updatedAt: string | null
}

export interface GameOverrideDistributionEntry {
  side: 'BLUE' | 'RED'
  position: 'TOP' | 'JUNGLE' | 'MID' | 'ADC' | 'SUPPORT'
  percent: number
  perMinute: number
}

// null 필드 = 변경하지 않음. 분배 빈 배열 = override 제거.
export interface GameOverridePayload {
  durationSec?: number | null
  blueTeamGold?: number | null
  redTeamGold?: number | null
  goldDistribution?: GameOverrideDistributionEntry[] | null
  damageDistribution?: GameOverrideDistributionEntry[] | null
  note?: string | null
}

export interface MatchExternalDetailPublicResponse {
  available: boolean
  reason: string | null
  provider: string | null
  status: string | null
  sourceUrl: string | null
  patchVersion: string | null
  lastSyncedAt: string | null
  games: MatchExternalDetailPublicGame[]
}

export interface MatchExternalDetailSyncItemResponse {
  matchId: number
  status: MatchExternalDetailStatus | 'FAILED'
  message: string
  detailSummary: MatchExternalDetailSummaryResponse | null
}

export interface MatchExternalDetailCandidateResponse {
  providerGameId: string
  sourceUrl: string
  score: number
  reasons: string[]
  autoSelected: boolean
}

export interface MatchExternalDetailCandidatesResponse {
  matchId: number
  status: MatchExternalDetailStatus | 'FAILED'
  autoSelectedSourceUrl: string | null
  autoSelectedScore: number | null
  candidates: MatchExternalDetailCandidateResponse[]
  detailSummary: MatchExternalDetailSummaryResponse | null
}

export interface MatchExternalDetailValidationResponse {
  valid: boolean
  normalizedSourceUrl: string | null
  providerGameId: string | null
  score: number
  reasons: string[]
  message: string
}

export interface MatchExternalDetailBatchSyncResponse {
  requestedCount: number
  syncedCount: number
  failedCount: number
  items: MatchExternalDetailSyncItemResponse[]
}

export type MatchExternalDetailAutoBindStatus =
  | 'BOUND'
  | 'AMBIGUOUS'
  | 'NO_CANDIDATE'
  | 'ALREADY_BOUND'
  | 'FAILED'

export interface MatchExternalDetailAutoBindItemResponse {
  matchId: number
  status: MatchExternalDetailAutoBindStatus
  message: string
  sourceUrl: string | null
  score: number | null
  detailSummary: MatchExternalDetailSummaryResponse | null
}

export interface MatchExternalDetailAutoBindBatchResponse {
  requestedCount: number
  boundCount: number
  skippedCount: number
  failedCount: number
  items: MatchExternalDetailAutoBindItemResponse[]
}

export interface GolGgTournamentSourceResponse {
  id: number
  sourceUrl: string
  label: string | null
  status: 'PENDING' | 'SYNCED' | 'FAILED'
  candidateCount: number
  lastIndexedAt: string | null
  errorMessage: string | null
}

// 일괄 동기화 응답: 각 소스의 처리 결과와 누락 ID를 함께 반환.
export interface GolGgBulkSyncResponse {
  successCount: number
  failedCount: number
  results: GolGgTournamentSourceResponse[]
  missingIds: number[]
}

// 일괄 삭제 응답: 실제 삭제된 건수와 미존재 ID 목록.
export interface GolGgBulkDeleteResponse {
  deletedCount: number
  missingIds: number[]
}

export interface MatchResponse {
  id: number
  game: GameResponse
  teamA: TeamSummary
  teamB: TeamSummary
  tournamentName: string
  stage: string
  scheduledAt: string
  status: MatchStatus
  externalId: string | null
  externalSource: MatchExternalSource
  lastSyncedAt: string | null
  detailSummary: MatchExternalDetailSummaryResponse | null
  result: MatchResultResponse | null
}

export type PandaScorePreviewStatus =
  | 'NEW'
  | 'UPDATE'
  | 'TEAM_MATCH_FAILED'
  | 'CONFLICT'
  | 'REJECTED'
export type PandaScoreMatchPreviewType = 'upcoming' | 'completed'

export type PandaScoreImportResultStatus = 'CREATED' | 'UPDATED' | 'SKIPPED'

export type PandaScoreTeamMatchMethod = 'EXTERNAL_ID' | 'NAME_CANDIDATE' | 'NONE'

export interface PandaScoreTeamPreview {
  externalId: string | null
  name: string | null
  matchedTeamId: number | null
  matchedTeamName: string | null
  matchMethod: PandaScoreTeamMatchMethod
  confirmed?: boolean
}

export interface PandaScoreMatchPreviewResponse {
  externalId: string | null
  source: 'PANDASCORE'
  leagueCode: string | null
  leagueName: string | null
  previewStatus: PandaScorePreviewStatus
  tournamentName: string | null
  scheduledAt: string | null
  pandaStatus: string | null
  teamA: PandaScoreTeamPreview
  teamB: PandaScoreTeamPreview
  existingMatchId: number | null
  conflictReasons: string[]
}

export interface PandaScoreMatchImportItemResponse {
  externalId: string
  importStatus: PandaScoreImportResultStatus
  matchId: number | null
  message: string
}

export interface PandaScoreMatchImportResponse {
  requestedCount: number
  createdCount: number
  updatedCount: number
  skippedCount: number
  items: PandaScoreMatchImportItemResponse[]
}

export interface PandaScoreMatchResultSyncItemResponse {
  externalId: string | null
  syncStatus: PandaScoreImportResultStatus
  matchId: number | null
  message: string
}

export interface PandaScoreMatchResultSyncResponse {
  requestedCount: number
  createdCount: number
  updatedCount: number
  skippedCount: number
  items: PandaScoreMatchResultSyncItemResponse[]
}

export type PandaScoreTeamImportResultStatus = 'CREATED' | 'MATCHED' | 'UPDATED' | 'SKIPPED'

export interface PandaScoreTeamImportItemResponse {
  externalId: string | null
  teamName: string | null
  leagueCode: string
  resultStatus: PandaScoreTeamImportResultStatus
  teamId: number | null
  message: string
}

export interface PandaScoreTeamImportResponse {
  fetchedCount: number
  createdCount: number
  matchedCount: number
  updatedCount: number
  skippedCount: number
  items: PandaScoreTeamImportItemResponse[]
}

export interface TeamResponse {
  id: number
  name: string
  shortName: string | null
  league: string | null
  logoUrl: string | null
  instagramUrl: string | null
  xUrl: string | null
  youtubeUrl: string | null
  livePlatform: string | null
  liveUrl: string | null
  externalId: string | null
  gameId: number
  primaryColor: string | null
  secondaryColor: string | null
  players: PlayerResponse[] | null
}

export interface PlayerResponse {
  id: number
  inGameName: string
  realName: string | null
  role: string | null
  nationality: string | null
  birthDate: string | null
  profileImageUrl: string | null
  instagramUrl: string | null
  xUrl: string | null
  youtubeUrl: string | null
  status: PlayerStatus
  externalId: string | null
  externalSource: PlayerExternalSource
  lastSyncedAt: string | null
  teamId: number | null
}
