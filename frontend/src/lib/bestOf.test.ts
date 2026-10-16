import { describe, expect, it } from 'vitest'
import { formatBestOf } from './bestOf'

describe('formatBestOf', () => {
  it('값이 있으면 BO 표기로 변환한다', () => {
    expect(formatBestOf(3)).toBe('BO3')
    expect(formatBestOf(5)).toBe('BO5')
  })

  it('값이 없으면 null 을 반환한다', () => {
    expect(formatBestOf(null)).toBeNull()
    expect(formatBestOf(undefined)).toBeNull()
  })
})
