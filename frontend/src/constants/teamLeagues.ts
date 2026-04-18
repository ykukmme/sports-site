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

export function getTeamLeagueLabel(code: string | null | undefined): string {
  if (!code) return '-'
  return TEAM_LEAGUES.find((league) => league.code === code)?.label ?? code
}
