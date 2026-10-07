import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import type { MatchResponse } from '../types/domain'

export interface MatchDateGroup {
  key: string
  label: string
  items: MatchResponse[]
}

// 경기 목록을 날짜별로 묶기 — 입력 정렬 순서 유지
export function groupMatchesByDate(matches: MatchResponse[]): MatchDateGroup[] {
  const groups = new Map<string, MatchDateGroup>()

  for (const match of matches) {
    const date = new Date(match.scheduledAt)
    const valid = !isNaN(date.getTime())
    const key = valid ? format(date, 'yyyy-MM-dd') : 'unknown'
    const label = valid ? format(date, 'M월 d일 (EEE)', { locale: ko }) : '일정 미정'

    const group = groups.get(key)
    if (group) {
      group.items.push(match)
    } else {
      groups.set(key, { key, label, items: [match] })
    }
  }

  return [...groups.values()]
}

// 경기 시각 표시 — 잘못된 값이면 '일정 미정'
export function formatMatchTime(value: string, pattern = 'MM/dd HH:mm'): string {
  const date = new Date(value)
  return isNaN(date.getTime()) ? '일정 미정' : format(date, pattern)
}
