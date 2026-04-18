// 백엔드 API 응답 도메인 타입 정의
// 백엔드 Java record와 1:1 대응

// AI 하이라이트 요약 응답 DTO
export interface AiSummaryResponse {
  matchId: number
  summaryText: string
  modelVersion: string
  generatedAt: string
}

// 챗봇 응답 DTO
export interface ChatbotResponse {
  answer: string
}

// 종목 응답 DTO
export interface GameResponse {
  id: number
  name: string
  shortName: string
}

// 경기 결과 응답 DTO
export interface MatchResultResponse {
  scoreTeamA: number
  scoreTeamB: number
  winnerTeamId: number | null
  playedAt: string
  vodUrl: string | null
}

// 경기 내 팀 요약 (로고 없음 — 백엔드 TeamSummary record 기준)
export interface TeamSummary {
  id: number
  name: string
  shortName: string
}

// 경기 상태 — 백엔드 MatchStatus enum과 동일
export type MatchStatus = 'SCHEDULED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'
export type MatchExternalSource = 'MANUAL' | 'PANDASCORE'
export type PlayerStatus = 'ACTIVE' | 'INACTIVE' | 'RETIRED'
export type PlayerExternalSource = 'MANUAL' | 'PANDASCORE'

// 경기 응답 DTO
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
  // 결과가 없는 경기(SCHEDULED/ONGOING)에서는 null
  result: MatchResultResponse | null
}

export type PandaScorePreviewStatus =
  | 'NEW'
  | 'UPDATE'
  | 'TEAM_MATCH_FAILED'
  | 'CONFLICT'
  | 'REJECTED'

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
  previewStatus: PandaScorePreviewStatus
  tournamentName: string | null
  scheduledAt: string | null
  pandaStatus: string | null
  teamA: PandaScoreTeamPreview
  teamB: PandaScoreTeamPreview
  existingMatchId: number | null
  conflictReasons: string[]
}

// 팀 응답 DTO — players는 상세 조회 시에만 포함
export interface TeamResponse {
  id: number
  name: string
  shortName: string | null
  region: string | null
  logoUrl: string | null
  instagramUrl: string | null
  xUrl: string | null
  youtubeUrl: string | null
  livePlatform: string | null
  liveUrl: string | null
  gameId: number
  // 팬 테마용 팀 색상 — 어드민에서 미설정 시 null
  primaryColor: string | null
  secondaryColor: string | null
  // 목록 API에서는 null, 상세 API에서만 포함
  players: PlayerResponse[] | null
}

// 선수 응답 DTO
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
  // 팀 미소속(free agent) 선수의 경우 null
  teamId: number | null
}
