import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchStatQualityMatches, fetchStatQualitySummary } from '../api/admin'
import type { StatQualityIssueType } from '../types/domain'

export function useStatQualitySummary() {
  return useQuery({
    queryKey: ['admin', 'stats', 'quality', 'summary'],
    queryFn: fetchStatQualitySummary,
    staleTime: 30_000,
  })
}

export function useStatQualityMatches(page: number, type: StatQualityIssueType | 'ALL') {
  return useQuery({
    queryKey: ['admin', 'stats', 'quality', 'matches', page, type],
    queryFn: () => fetchStatQualityMatches(page, type),
    staleTime: 30_000,
    // 페이지·필터 전환 중 이전 결과를 유지 — 요약 카드와 필터가 언마운트되지 않게 한다
    placeholderData: keepPreviousData,
  })
}
