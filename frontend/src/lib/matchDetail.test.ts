import { describe, expect, it } from 'vitest'
import { getGameNo, getGameWinner, getTeamGameStats, resolveBlueTeam, sumTeamStats, formatDuration } from './matchDetail'
import { formatMatchTime, groupMatchesByDate } from './matchDate'
import type { MatchExternalDetailPublicGame, MatchResponse } from '../types/domain'

// 테스트용 경기 — 필요한 필드만 채움
const match = {
  id: 1,
  teamA: { id: 10, name: 'T1', shortName: 'T1' },
  teamB: { id: 20, name: 'Gen.G', shortName: 'GEN' },
  scheduledAt: '2026-05-01T09:00:00Z',
} as unknown as MatchResponse

// 테스트용 세트 — 기본값은 모두 null
function game(overrides: Partial<MatchExternalDetailPublicGame> = {}): MatchExternalDetailPublicGame {
  return {
    gameNo: null,
    blueTeamId: null,
    redTeamId: null,
    blueTeamName: null,
    winnerSide: null,
    blueKills: null,
    redKills: null,
    blueTeamGold: null,
    redTeamGold: null,
    blueTowers: null,
    redTowers: null,
    blueDragons: null,
    redDragons: null,
    blueBarons: null,
    redBarons: null,
    ...overrides,
  } as MatchExternalDetailPublicGame
}

describe('resolveBlueTeam', () => {
  it('blueTeamId로 진영 판별', () => {
    expect(resolveBlueTeam(game({ blueTeamId: 20 }), match)).toBe('B')
  })
  it('redTeamId만 있으면 반대 진영으로 판별', () => {
    expect(resolveBlueTeam(game({ redTeamId: 20 }), match)).toBe('A')
  })
  it('팀 이름(약칭) 대소문자 무시하고 판별', () => {
    expect(resolveBlueTeam(game({ blueTeamName: ' gen ' }), match)).toBe('B')
  })
  it('판별 불가하면 null — 추측하지 않음', () => {
    expect(resolveBlueTeam(game({ blueTeamName: 'Unknown' }), match)).toBeNull()
    expect(resolveBlueTeam(game(), match)).toBeNull()
  })
})

describe('getGameWinner', () => {
  it('RED 승리 + 블루가 A면 B 승리', () => {
    expect(getGameWinner(game({ blueTeamId: 10, winnerSide: 'red' }), match)).toBe('B')
  })
  it('진영 판별 불가면 null', () => {
    expect(getGameWinner(game({ winnerSide: 'BLUE' }), match)).toBeNull()
  })
})

describe('getTeamGameStats / sumTeamStats', () => {
  const g1 = game({ blueTeamId: 10, blueKills: 10, redKills: 5, blueTeamGold: 60000, redTeamGold: 50000, blueTowers: 9, redTowers: 2, blueDragons: 3, redDragons: 1, blueBarons: 1, redBarons: 0 })
  const g2 = game({ blueTeamId: 20, blueKills: 7, redKills: 12, blueTeamGold: 52000, redTeamGold: 61000, blueTowers: 3, redTowers: 10, blueDragons: 1, redDragons: 4, blueBarons: 0, redBarons: 2 })

  it('진영에 맞춰 팀 기록 매핑', () => {
    expect(getTeamGameStats(g2, match, 'A')?.kills).toBe(12)
  })
  it('세트 합산', () => {
    const sum = sumTeamStats([getTeamGameStats(g1, match, 'A'), getTeamGameStats(g2, match, 'A')])
    expect(sum).toEqual({ kills: 22, gold: 121000, towers: 19, dragons: 7, barons: 3 })
  })
  it('한 세트라도 값이 없으면 해당 항목은 null — 보간 금지', () => {
    const g3 = game({ blueTeamId: 10, blueKills: 1 })
    const sum = sumTeamStats([getTeamGameStats(g1, match, 'A'), getTeamGameStats(g3, match, 'A')])
    expect(sum?.kills).toBe(11)
    expect(sum?.gold).toBeNull()
  })
  it('진영 판별 불가 세트가 있으면 전체 null', () => {
    expect(sumTeamStats([getTeamGameStats(g1, match, 'A'), getTeamGameStats(game(), match, 'A')])).toBeNull()
    expect(sumTeamStats([])).toBeNull()
  })
})

describe('포맷 함수', () => {
  it('gameNo 누락 시 순서로 대체', () => {
    expect(getGameNo(game(), 2)).toBe(3)
    expect(getGameNo(game({ gameNo: 5 }), 0)).toBe(5)
  })
  it('경기 시간 포맷', () => {
    expect(formatDuration(1865)).toBe('31:05')
    expect(formatDuration(null)).toBe('-')
  })
  it('잘못된 일정은 일정 미정', () => {
    expect(formatMatchTime('invalid')).toBe('일정 미정')
  })
  it('날짜별 그룹은 입력 순서 유지', () => {
    const list = [
      { ...match, id: 1, scheduledAt: '2026-05-01T03:00:00' },
      { ...match, id: 2, scheduledAt: '2026-05-02T03:00:00' },
      { ...match, id: 3, scheduledAt: '2026-05-01T05:00:00' },
      { ...match, id: 4, scheduledAt: 'bad' },
    ] as MatchResponse[]
    const groups = groupMatchesByDate(list)
    expect(groups.map((group) => group.key)).toEqual(['2026-05-01', '2026-05-02', 'unknown'])
    expect(groups[0].items.map((item) => item.id)).toEqual([1, 3])
  })
})
