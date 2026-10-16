// 어드민 단계 복구 입력 — 쉼표·공백·줄바꿈으로 구분된 tournament id 파싱

export const MAX_BACKFILL_IDS = 100

export interface ParsedTournamentIds {
  ids: number[]
  invalid: string[]
}

// 양의 정수만 허용, 중복 제거, 입력 순서 유지
export function parseTournamentIds(text: string): ParsedTournamentIds {
  const ids: number[] = []
  const invalid: string[] = []
  const seen = new Set<number>()
  for (const token of text.split(/[\s,]+/)) {
    if (!token) continue
    if (!/^\d+$/.test(token) || Number(token) <= 0 || !Number.isSafeInteger(Number(token))) {
      invalid.push(token)
      continue
    }
    const id = Number(token)
    if (!seen.has(id)) {
      seen.add(id)
      ids.push(id)
    }
  }
  return { ids, invalid }
}
