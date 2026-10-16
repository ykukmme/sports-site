import { describe, expect, it } from 'vitest'
import { parseTournamentIds } from './stageIds'

describe('parseTournamentIds', () => {
  it('쉼표·공백·줄바꿈 구분을 모두 허용하고 중복을 제거한다', () => {
    expect(parseTournamentIds('22046, 22047\n21722  22046')).toEqual({
      ids: [22046, 22047, 21722],
      invalid: [],
    })
  })

  it('숫자가 아니거나 0 이하인 값은 invalid 로 분리한다', () => {
    expect(parseTournamentIds('abc, 0, -5, 12.5, 7')).toEqual({
      ids: [7],
      invalid: ['abc', '0', '-5', '12.5'],
    })
  })

  it('빈 입력은 빈 결과', () => {
    expect(parseTournamentIds('  \n ')).toEqual({ ids: [], invalid: [] })
  })
})
