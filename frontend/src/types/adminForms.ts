// 어드민 폼 zod 스키마 + 타입 정의
// 클라이언트 사이드 검증 (Hard Rule #3 — input validation)
import { z } from 'zod'

// 경기 등록 스키마
export const matchCreateSchema = z.object({
  gameId: z.number({ message: '종목을 선택해주세요.' }).int().positive(),
  teamAId: z.number({ message: '팀 A를 선택해주세요.' }).int().positive(),
  teamBId: z.number({ message: '팀 B를 선택해주세요.' }).int().positive(),
  tournamentName: z.string().min(1, '대회명을 입력해주세요.'),
  stage: z.string().optional(),
  scheduledAt: z.string().min(1, '예정 시각을 입력해주세요.'),
})

export type MatchCreateFormValues = z.infer<typeof matchCreateSchema>

// 경기 수정 스키마 (상태 변경 포함)
export const matchUpdateSchema = z.object({
  tournamentName: z.string().min(1, '대회명을 입력해주세요.').optional(),
  stage: z.string().optional(),
  scheduledAt: z.string().optional(),
  status: z.enum(['SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED']).optional(),
})

export type MatchUpdateFormValues = z.infer<typeof matchUpdateSchema>

// 경기 결과 입력 스키마
export const matchResultSchema = z.object({
  winnerTeamId: z
    .number({ message: '승리 팀을 선택해주세요.' })
    .int()
    .positive()
    .nullable()
    .optional(),
  scoreTeamA: z.number({ message: '점수를 입력해주세요.' }).int().min(0),
  scoreTeamB: z.number({ message: '점수를 입력해주세요.' }).int().min(0),
  playedAt: z.string().min(1, '경기 시각을 입력해주세요.'),
  vodUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  notes: z.string().max(1000, '비고는 1000자 이내로 입력해주세요.').optional(),
})

export type MatchResultFormValues = z.infer<typeof matchResultSchema>

// 팀 등록/수정 스키마
export const teamFormSchema = z.object({
  name: z.string().min(1, '팀명을 입력해주세요.'),
  shortName: z.string().optional(),
  region: z.string().optional(),
  logoUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  instagramUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  xUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  youtubeUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  livePlatform: z.string().optional(),
  liveUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  gameId: z.number({ message: '종목을 선택해주세요.' }).int().positive(),
  primaryColor: z
    .string()
    .regex(/^#[0-9A-Fa-f]{6}$/, '#RRGGBB 형식으로 입력해주세요.')
    .or(z.literal(''))
    .optional(),
  secondaryColor: z
    .string()
    .regex(/^#[0-9A-Fa-f]{6}$/, '#RRGGBB 형식으로 입력해주세요.')
    .or(z.literal(''))
    .optional(),
})

export type TeamFormValues = z.infer<typeof teamFormSchema>

// 선수 등록/수정 스키마
export const playerFormSchema = z.object({
  inGameName: z.string().min(1, '닉네임을 입력해주세요.'),
  realName: z.string().optional(),
  role: z.string().optional(),
  nationality: z.string().optional(),
  profileImageUrl: z.string().url('올바른 URL을 입력해주세요.').or(z.literal('')).optional(),
  teamId: z.number().int().positive().nullable().optional(),
})

export type PlayerFormValues = z.infer<typeof playerFormSchema>
