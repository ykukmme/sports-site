import { describe, expect, it } from 'vitest'
import { groupTeamsByStatus, publicCoverageNotice } from './swissCoverage'
import type { SwissTeamStatus, SwissTeamStatusResponse } from '../types/domain'

// 테스트용 팀 현황 — 필요한 필드만 채움
function team(id: number, status: SwissTeamStatus, wins = 0, losses = 0): SwissTeamStatusResponse {
  return {
    team: { id, name: `Team ${id}`, shortName: `T${id}`, logoUrl: null },
    wins,
    losses,
    status,
  } as SwissTeamStatusResponse
}

describe('publicCoverageNotice', () => {
  it('완전하면 안내를 표시하지 않는다', () => {
    expect(publicCoverageNotice('COMPLETE')).toBeNull()
  })

  it('불완전·미확인이면 한 줄 안내만 표시한다', () => {
    expect(publicCoverageNotice('INCOMPLETE')).toBe('일부 경기 결과 확인 중')
    expect(publicCoverageNotice('UNKNOWN')).toBe('일부 경기 결과 확인 중')
  })
})

describe('groupTeamsByStatus', () => {
  it('확인 중 팀을 별도 묶음으로 마지막에 둔다', () => {
    const groups = groupTeamsByStatus(
      [team(1, 'UNKNOWN', 2, 1), team(2, 'QUALIFIED', 3, 0), team(3, 'ALIVE', 1, 1)],
      3,
      3,
    )

    expect(groups.map((g) => g.label)).toEqual(['진출 (3승)', '진행 중', '확인 중'])
    const last = groups[groups.length - 1]
    expect(last.unknown).toBe(true)
    expect(last.items.map((t) => t.team.id)).toEqual([1])
  })

  it('전 팀이 확인 중이면 진출·탈락 묶음이 없다', () => {
    const groups = groupTeamsByStatus([team(1, 'UNKNOWN', 3, 0), team(2, 'UNKNOWN', 0, 3)], 3, 3)

    expect(groups).toHaveLength(1)
    expect(groups[0].label).toBe('확인 중')
  })
})
