export const TEAM_LEAGUES = [
  { code: 'LCK', label: 'LCK' },
  { code: 'LPL', label: 'LPL' },
  { code: 'LEC', label: 'LEC' },
  { code: 'LCS', label: 'LCS' },
  { code: 'LCP', label: 'LCP' },
  { code: 'CBLOL', label: 'CBLOL' },
  { code: 'LCK_CL', label: 'LCK CL' },
] as const

export type TeamLeagueCode = (typeof TEAM_LEAGUES)[number]['code']
export const INTERNATIONAL_LEAGUE_CODE = 'INTERNATIONAL' as const
export type MatchLeagueFilterCode = TeamLeagueCode | typeof INTERNATIONAL_LEAGUE_CODE

export const MATCH_LEAGUE_FILTERS = [
  ...TEAM_LEAGUES,
  { code: INTERNATIONAL_LEAGUE_CODE, label: '국제전' },
] as const

export function getTeamLeagueLabel(code: string | null | undefined): string {
  if (!code) return '-'
  if (code === INTERNATIONAL_LEAGUE_CODE) return '국제전'
  return TEAM_LEAGUES.find((league) => league.code === code)?.label ?? code
}
