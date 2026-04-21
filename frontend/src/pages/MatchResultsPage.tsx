import { useMemo, useState } from 'react'
import { useMatchResults } from '../hooks/useMatches'
import { useTeams } from '../hooks/useTeams'
import { MatchList } from '../components/match/MatchList'
import { Button } from '@/components/ui/button'
import { TEAM_LEAGUES } from '../constants/teamLeagues'

export function MatchResultsPage() {
  const { data, isLoading, error } = useMatchResults()
  const { data: teams } = useTeams()

  const [selectedLeague, setSelectedLeague] = useState<string>('ALL')
  const [selectedTeamId, setSelectedTeamId] = useState<string>('ALL')
  const [sinceDate, setSinceDate] = useState<string>('')

  const teamLeagueById = useMemo(() => {
    const map = new Map<number, string>()
    ;(teams ?? []).forEach((team) => {
      if (team.league) {
        map.set(team.id, team.league)
      }
    })
    return map
  }, [teams])

  const teamsInLeague = useMemo(() => {
    const source = teams ?? []
    const filtered =
      selectedLeague === 'ALL'
        ? source
        : source.filter((team) => team.league === selectedLeague)

    return [...filtered].sort((a, b) => a.name.localeCompare(b.name))
  }, [teams, selectedLeague])

  const filteredMatches = useMemo(() => {
    if (!data) return []

    const selectedTeamNumber = selectedTeamId === 'ALL' ? null : Number(selectedTeamId)
    const thresholdDate = sinceDate ? new Date(`${sinceDate}T00:00:00`) : null

    return data.filter((match) => {
      if (selectedLeague !== 'ALL') {
        const teamALeague = teamLeagueById.get(match.teamA.id)
        const teamBLeague = teamLeagueById.get(match.teamB.id)
        if (teamALeague !== selectedLeague && teamBLeague !== selectedLeague) {
          return false
        }
      }

      if (selectedTeamNumber !== null) {
        if (match.teamA.id !== selectedTeamNumber && match.teamB.id !== selectedTeamNumber) {
          return false
        }
      }

      const scheduledAt = new Date(match.scheduledAt)
      if (isNaN(scheduledAt.getTime())) {
        return false
      }

      if (thresholdDate && scheduledAt < thresholdDate) {
        return false
      }

      return true
    })
  }, [data, selectedLeague, selectedTeamId, sinceDate, teamLeagueById])

  const availableLeagueCodes = useMemo(() => {
    const codes = new Set((teams ?? []).map((team) => team.league).filter(Boolean))
    return TEAM_LEAGUES.filter((league) => codes.has(league.code))
  }, [teams])

  const handleLeagueChange = (league: string) => {
    setSelectedLeague(league)
    if (selectedTeamId === 'ALL') return

    const selectedTeam = (teams ?? []).find((team) => team.id === Number(selectedTeamId))
    if (!selectedTeam) {
      setSelectedTeamId('ALL')
      return
    }

    if (league !== 'ALL' && selectedTeam.league !== league) {
      setSelectedTeamId('ALL')
    }
  }

  const resetFilters = () => {
    setSelectedLeague('ALL')
    setSelectedTeamId('ALL')
    setSinceDate('')
  }

  return (
    <div>
      <h1 className="mb-6 text-4xl font-semibold leading-tight">경기 결과</h1>

      <div className="mb-4 grid gap-3 rounded-lg border border-border bg-card p-4 md:grid-cols-3">
        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">리그</span>
          <select
            className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
            value={selectedLeague}
            onChange={(event) => handleLeagueChange(event.target.value)}
          >
            <option value="ALL">전체</option>
            {availableLeagueCodes.map((league) => (
              <option key={league.code} value={league.code}>
                {league.label}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">팀</span>
          <select
            className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
            value={selectedTeamId}
            onChange={(event) => setSelectedTeamId(event.target.value)}
          >
            <option value="ALL">전체</option>
            {teamsInLeague.map((team) => (
              <option key={team.id} value={team.id}>
                {team.name}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block text-muted-foreground">기준일 이후</span>
          <div className="flex gap-2">
            <input
              type="date"
              className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm"
              value={sinceDate}
              onChange={(event) => setSinceDate(event.target.value)}
            />
            <Button type="button" variant="outline" onClick={resetFilters}>
              초기화
            </Button>
          </div>
        </label>
      </div>

      <p className="mb-4 text-sm text-muted-foreground">필터 결과: {filteredMatches.length}건</p>

      <MatchList matches={filteredMatches} isLoading={isLoading} error={error} />
    </div>
  )
}
