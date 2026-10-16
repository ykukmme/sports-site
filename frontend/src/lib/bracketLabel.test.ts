import { describe, expect, it } from 'vitest'
import { formatRoundLabel, slotDisplay } from './bracketLabel'
import type { BracketNodeResponse, MatchResponse } from '../types/domain'

// 테스트용 노드 — 필요한 필드만 채움
function node(overrides: Partial<BracketNodeResponse> = {}): BracketNodeResponse {
  return {
    id: 1,
    roundLabel: 'Grand final',
    sequence: null,
    section: 'FINAL',
    column: 3,
    row: 0,
    bestOf: 5,
    scheduledAt: null,
    match: null,
    slots: [],
    ...overrides,
  }
}

describe('formatRoundLabel', () => {
  it('PandaScore 라운드 라벨을 한국어로 바꾼다', () => {
    expect(formatRoundLabel('Upper bracket semifinal', 2)).toBe('승자조 4강 2경기')
    expect(formatRoundLabel('Lower bracket round 1')).toBe('패자조 1라운드')
    expect(formatRoundLabel('Upper bracket final')).toBe('승자조 결승')
    expect(formatRoundLabel('Grand Final')).toBe('결승전')
    expect(formatRoundLabel('Quarterfinal', 3)).toBe('8강 3경기')
  })

  it('모르는 라벨은 원문을 유지한다', () => {
    expect(formatRoundLabel('Tiebreaker')).toBe('Tiebreaker')
  })
})

describe('slotDisplay', () => {
  it('연결 경기가 있으면 팀과 점수를 표시한다', () => {
    const match = {
      id: 9,
      teamA: { id: 10, name: 'Gen.G', shortName: 'GEN' },
      teamB: { id: 20, name: 'Hanwha Life', shortName: 'HLE' },
      result: { winnerTeamId: 10, scoreTeamA: 3, scoreTeamB: 1 },
    } as unknown as MatchResponse

    const display = slotDisplay(node({ match }), 0)

    expect(display).toMatchObject({ kind: 'team', teamId: 10, text: 'GEN', score: 3, isWinner: true })
  })

  it('선행 경기 승자/패자 문구를 만든다', () => {
    const display = slotDisplay(
      node({
        slots: [
          {
            slotIndex: 1,
            sourceType: 'LOSER_OF',
            sourceNodeId: 5,
            sourceRoundLabel: 'Upper bracket final',
            sourceSequence: null,
            label: null,
            origin: 'AUTO',
          },
        ],
      }),
      1,
    )

    expect(display).toEqual({ kind: 'source', text: '승자조 결승 패자' })
  })

  it('어드민 문구를 표시한다', () => {
    const display = slotDisplay(
      node({
        slots: [
          {
            slotIndex: 0,
            sourceType: 'LABEL',
            sourceNodeId: null,
            sourceRoundLabel: null,
            sourceSequence: null,
            label: '레전드 1위',
            origin: 'ADMIN',
          },
        ],
      }),
      0,
    )

    expect(display).toEqual({ kind: 'label', text: '레전드 1위' })
  })

  it('정보가 없으면 미정으로 표시한다', () => {
    expect(slotDisplay(node(), 0)).toEqual({ kind: 'unknown', text: '미정' })
  })
})
