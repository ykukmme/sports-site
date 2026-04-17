import { z } from 'zod'

const urlMessage = '올바른 URL을 입력해주세요.'
const optionalUrl = z.string().url(urlMessage).or(z.literal('')).optional()
const rosterRoles = ['TOP', 'JGL', 'MID', 'BOT', 'SPT', 'HEAD COACH', 'COACH'] as const
const playerStatuses = ['ACTIVE', 'INACTIVE', 'RETIRED'] as const
const playerExternalSources = ['MANUAL', 'PANDASCORE'] as const

const logoUrlSchema = z
  .string()
  .refine(
    (value) => value === '' || value.startsWith('/uploads/team-logos/') || z.string().url().safeParse(value).success,
    urlMessage,
  )
  .optional()

const profileImageUrlSchema = z
  .string()
  .refine(
    (value) => value === '' || value.startsWith('/uploads/player-images/') || z.string().url().safeParse(value).success,
    urlMessage,
  )
  .optional()

export const matchCreateSchema = z.object({
  gameId: z.number({ message: '종목을 선택해주세요.' }).int().positive('종목을 선택해주세요.'),
  teamAId: z.number({ message: '팀 A를 선택해주세요.' }).int().positive('팀 A를 선택해주세요.'),
  teamBId: z.number({ message: '팀 B를 선택해주세요.' }).int().positive('팀 B를 선택해주세요.'),
  tournamentName: z.string().min(1, '대회명을 입력해주세요.'),
  stage: z.string().optional(),
  scheduledAt: z.string().min(1, '예정 시각을 입력해주세요.'),
})

export type MatchCreateFormValues = z.infer<typeof matchCreateSchema>

export const matchUpdateSchema = z.object({
  tournamentName: z.string().min(1, '대회명을 입력해주세요.').optional(),
  stage: z.string().optional(),
  scheduledAt: z.string().optional(),
  status: z.enum(['SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED']).optional(),
})

export type MatchUpdateFormValues = z.infer<typeof matchUpdateSchema>

export const matchResultSchema = z.object({
  winnerTeamId: z
    .number({ message: '승리 팀을 선택해주세요.' })
    .int()
    .positive('승리 팀을 선택해주세요.'),
  scoreTeamA: z.number({ message: '점수를 입력해주세요.' }).int().min(0, '점수는 0 이상이어야 합니다.'),
  scoreTeamB: z.number({ message: '점수를 입력해주세요.' }).int().min(0, '점수는 0 이상이어야 합니다.'),
  playedAt: z.string().min(1, '경기 시각을 입력해주세요.'),
  vodUrl: optionalUrl,
  notes: z.string().max(1000, '비고는 1000자 이내로 입력해주세요.').optional(),
})

export type MatchResultFormValues = z.infer<typeof matchResultSchema>

export const teamFormSchema = z.object({
  name: z.string().min(1, '팀명을 입력해주세요.'),
  shortName: z.string().optional(),
  region: z.string().optional(),
  logoUrl: logoUrlSchema,
  instagramUrl: optionalUrl,
  xUrl: optionalUrl,
  youtubeUrl: optionalUrl,
  livePlatform: z.string().optional(),
  liveUrl: optionalUrl,
  gameId: z.number({ message: '종목을 선택해주세요.' }).int().positive('종목을 선택해주세요.'),
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

export const playerFormSchema = z.object({
  inGameName: z.string().min(1, '닉네임을 입력해주세요.'),
  realName: z.string().optional(),
  role: z.enum(rosterRoles, { message: '역할을 선택해주세요.' }).or(z.literal('')).optional(),
  nationality: z.string().optional(),
  birthDate: z.string().regex(/^$|^\d{4}-\d{2}-\d{2}$/, '생년월일은 YYYY-MM-DD 형식으로 입력해주세요.').optional(),
  profileImageUrl: profileImageUrlSchema,
  instagramUrl: optionalUrl,
  xUrl: optionalUrl,
  youtubeUrl: optionalUrl,
  status: z.enum(playerStatuses),
  externalSource: z.enum(playerExternalSources),
  teamId: z.number().int().positive().nullable().optional(),
})

export type PlayerFormValues = z.infer<typeof playerFormSchema>
