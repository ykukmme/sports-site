import type { CoverageIssueType, CoverageStatus, SwissTeamStatusResponse } from '../types/domain'

// 공개 화면 안내 문구 — 완전하지 않을 때만 한 줄 표시 (상세 사유는 어드민 전용)
export function publicCoverageNotice(status: CoverageStatus): string | null {
  return status === 'COMPLETE' ? null : '일부 경기 결과 확인 중'
}

// 어드민 완전성 상태 라벨
export const COVERAGE_STATUS_LABELS: Record<CoverageStatus, string> = {
  UNKNOWN: '미확인 (원본 동기화 필요)',
  INCOMPLETE: '불완전',
  COMPLETE: '일치',
}

// 어드민 불일치 사유 라벨
export const COVERAGE_ISSUE_LABELS: Record<CoverageIssueType, string> = {
  MISSING_LOCAL: '로컬 경기 없음',
  MISSING_RESULT: '로컬 결과 없음',
  WINNER_MISMATCH: '승자 불일치',
  WINNER_UNVERIFIABLE: '승자 대조 불가',
  EXTRA_LOCAL: '원본에 없는 로컬 경기',
}

// 팀 현황 묶음 — 진출 / 진행 중 / 탈락 / 확인 중 순서, 빈 묶음 제외
export function groupTeamsByStatus(
  teams: SwissTeamStatusResponse[],
  winTarget: number,
  lossTarget: number,
): { label: string; unknown: boolean; items: SwissTeamStatusResponse[] }[] {
  return [
    { label: `진출 (${winTarget}승)`, unknown: false, items: teams.filter((t) => t.status === 'QUALIFIED') },
    { label: '진행 중', unknown: false, items: teams.filter((t) => t.status === 'ALIVE') },
    { label: `탈락 (${lossTarget}패)`, unknown: false, items: teams.filter((t) => t.status === 'ELIMINATED') },
    { label: '확인 중', unknown: true, items: teams.filter((t) => t.status === 'UNKNOWN') },
  ].filter((group) => group.items.length > 0)
}
