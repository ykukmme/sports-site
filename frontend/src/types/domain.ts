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
  // 결과가 없는 경기(SCHEDULED/ONGOING)에서는 null
  result: MatchResultResponse | null
}

// 팀 응답 DTO — players는 상세 조회 시에만 포함
export interface TeamResponse {
  id: number
  name: string
  shortName: string
  region: string
  logoUrl: string | null
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
  profileImageUrl: string | null
  // 팀 미소속(free agent) 선수의 경우 null
  teamId: number | null
}
