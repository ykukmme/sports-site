import { useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { MatchList } from '../components/match/MatchList'
import { MATCH_LEAGUE_FILTERS, isInternationalLeagueCode } from '../constants/teamLeagues'
import { useMatchResultsPage } from '../hooks/useMatches'
import { useTeams } from '../hooks/useTeams'

export function MatchResultsPage() {
  const [page, setPage] = useState(0)
  const [selectedLeague, setSelectedLeague] = useState<string>('ALL')
  const [selectedTeamId, setSelectedTeamId] = useState<string>('ALL')
  const [sinceDate, setSinceDate] = useState<string>('')

  const { data: teams } = useTeams()
  const selectedTeamNumber = selectedTeamId === 'ALL' ? undefined : Number(selectedTeamId)
  const selectedLeagueParam = selectedLeague === 'ALL' ? undefined : selectedLeague
  const resultsQuery = useMatchResultsPage(page, selectedLeagueParam, selectedTeamNumber, sinceDate || undefined)

  const teamsInLeague = useMemo(() => {
    const source = teams ?? []
    const filtered =
      selectedLeague === 'ALL' || isInternationalLeagueCode(selectedLeague)
        ? source
        : source.filter((team) => (team.league ?? '').toUpperCase() === selectedLeague)
    return [...filtered].sort((a, b) => a.name.localeCompare(b.name))
  }, [teams, selectedLeague])

  const availableLeagueCodes = useMemo(() => MATCH_LEAGUE_FILTERS, [])

  const handleLeagueChange = (league: string) => {
    setSelectedLeague(league)
    setPage(0)
    if (selectedTeamId === 'ALL') {
      return
    }
    const selectedTeam = (teams ?? []).find((team) => team.id === Number(selectedTeamId))
    if (!selectedTeam) {
      setSelectedTeamId('ALL')
      return
    }
    if (
      league !== 'ALL' &&
      !isInternationalLeagueCode(league) &&
      (selectedTeam.league ?? '').toUpperCase() !== league
    ) {
      setSelectedTeamId('ALL')
    }
  }

  const resetFilters = () => {
    setSelectedLeague('ALL')
    setSelectedTeamId('ALL')
    setSinceDate('')
    setPage(0)
  }

  const pageData = resultsQuery.data
  const matches = pageData?.content ?? []

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
            onChange={(event) => {
              setSelectedTeamId(event.target.value)
              setPage(0)
            }}
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
              onChange={(event) => {
                setSinceDate(event.target.value)
                setPage(0)
              }}
            />
            <Button type="button" variant="outline" onClick={resetFilters}>
              초기화
            </Button>
          </div>
        </label>
      </div>

      <p className="mb-4 text-sm text-muted-foreground">
        전체 {pageData?.totalElements ?? 0}건
      </p>

      <MatchList matches={matches} isLoading={resultsQuery.isLoading} error={resultsQuery.error} />

      <div className="mt-4 flex justify-end gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={pageData?.first ?? true}
          onClick={() => setPage((prev) => Math.max(0, prev - 1))}
        >
          이전
        </Button>
        <span className="flex items-center text-sm text-muted-foreground">
          {(pageData?.number ?? 0) + 1} / {pageData?.totalPages ?? 1}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={pageData?.last ?? true}
          onClick={() => setPage((prev) => prev + 1)}
        >
          다음
        </Button>
      </div>
    </div>
  )
}
