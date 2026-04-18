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
