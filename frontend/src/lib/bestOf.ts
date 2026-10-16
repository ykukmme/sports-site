// BO(Best-of) 표기 — 값이 없으면 null (표시하지 않음)
export function formatBestOf(bestOf: number | null | undefined): string | null {
  if (bestOf == null || bestOf <= 0) return null
  return `BO${bestOf}`
}
