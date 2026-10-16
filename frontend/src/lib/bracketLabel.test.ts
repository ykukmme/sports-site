import { describe, expect, it } from 'vitest'
import { buildSlotContext, formatRoundLabel, slotDisplays } from './bracketLabel'
import type { BracketEdgeResponse, BracketNodeResponse, MatchResponse } from '../types/domain'

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

function edge(overrides: Partial<BracketEdgeResponse>): BracketEdgeResponse {
  return { id: 1, sourceNodeId: 0, targetNodeId: 1, outcome: 'WINNER', origin: 'AUTO', targetSlotIndex: null, ...overrides }
}

// 4강 1·2경기 → 결승 대진
const semifinal1 = node({ id: 11, roundLabel: 'Semifinal', sequence: 1, section: 'UPPER', column: 2, row: 0 })
const semifinal2 = node({ id: 12, roundLabel: 'Semifinal', sequence: 2, section: 'UPPER', column: 2, row: 1 })

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

describe('slotDisplays', () => {
  it('연결 경기가 있으면 팀과 점수를 표시한다 (간선보다 우선)', () => {
    const match = {
      id: 9,
      teamA: { id: 10, name: 'Gen.G', shortName: 'GEN' },
      teamB: { id: 20, name: 'Hanwha Life', shortName: 'HLE' },
      result: { winnerTeamId: 10, scoreTeamA: 3, scoreTeamB: 1 },
    } as unknown as MatchResponse
    const finalNode = node({ match })
    const context = buildSlotContext([semifinal1, finalNode], [edge({ sourceNodeId: 11, targetSlotIndex: 1 })])

    const [first] = slotDisplays(finalNode, context)

    expect(first).toMatchObject({ kind: 'team', teamId: 10, text: 'GEN', score: 3, isWinner: true })
  })

  it('미확정 간선은 응답 순서가 아니라 출처 경기 순서로 빈 줄을 채운다', () => {
    const finalNode = node()
    // 응답에는 4강 2경기 간선이 먼저 와도
    const context = buildSlotContext(
      [semifinal1, semifinal2, finalNode],
      [edge({ id: 1, sourceNodeId: 12 }), edge({ id: 2, sourceNodeId: 11 })],
    )

    expect(slotDisplays(finalNode, context)).toEqual([
      { kind: 'source', text: '4강 1경기 승자' },
      { kind: 'source', text: '4강 2경기 승자' },
    ])
  })

  it('팀 줄이 확정된 간선은 그 줄에 두고 나머지를 빈 줄에 채운다', () => {
    const finalNode = node()
    const context = buildSlotContext(
      [semifinal1, semifinal2, finalNode],
      [edge({ id: 1, sourceNodeId: 11, targetSlotIndex: 1, origin: 'ADMIN' }), edge({ id: 2, sourceNodeId: 12 })],
    )

    expect(slotDisplays(finalNode, context)).toEqual([
      { kind: 'source', text: '4강 2경기 승자' },
      { kind: 'source', text: '4강 1경기 승자' },
    ])
  })

  it('어드민 문구가 있는 줄은 문구를 표시하고 패자 진출 문구도 만든다', () => {
    const finalNode = node({ slots: [{ slotIndex: 0, label: '레전드 1위' }] })
    const context = buildSlotContext([semifinal1, finalNode], [edge({ sourceNodeId: 11, outcome: 'LOSER' })])

    expect(slotDisplays(finalNode, context)).toEqual([
      { kind: 'label', text: '레전드 1위' },
      { kind: 'source', text: '4강 1경기 패자' },
    ])
  })

  it('정보가 없으면 미정 (추정하지 않음)', () => {
    expect(slotDisplays(node(), buildSlotContext([], []))).toEqual([
      { kind: 'unknown', text: '미정' },
      { kind: 'unknown', text: '미정' },
    ])
  })
})
