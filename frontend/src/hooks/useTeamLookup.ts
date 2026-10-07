import { useMemo } from 'react'
import { useTeams } from './useTeams'
import type { TeamResponse } from '../types/domain'

// 팀 ID → 팀 상세(로고·색상·리그) 조회 맵 — useTeams 캐시 재사용
export function useTeamLookup(): Map<number, TeamResponse> {
  const { data } = useTeams()
  return useMemo(() => new Map((data ?? []).map((team) => [team.id, team])), [data])
}
