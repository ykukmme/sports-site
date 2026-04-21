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
export const INTERNATIONAL_FIRST_STAND_CODE = 'INTERNATIONAL_FIRST_STAND' as const
export const INTERNATIONAL_MSI_CODE = 'INTERNATIONAL_MSI' as const
export const INTERNATIONAL_WORLDS_CODE = 'INTERNATIONAL_WORLDS' as const

export const INTERNATIONAL_LEAGUE_FILTERS = [
  { code: INTERNATIONAL_FIRST_STAND_CODE, label: 'FIRST STAND' },
  { code: INTERNATIONAL_MSI_CODE, label: 'MSI' },
  { code: INTERNATIONAL_WORLDS_CODE, label: 'WORLDS' },
] as const

export type InternationalLeagueFilterCode = (typeof INTERNATIONAL_LEAGUE_FILTERS)[number]['code']
export type InternationalLeagueCode = typeof INTERNATIONAL_LEAGUE_CODE | InternationalLeagueFilterCode
export type MatchLeagueFilterCode = TeamLeagueCode | InternationalLeagueFilterCode

export const MATCH_LEAGUE_FILTERS = [
  ...TEAM_LEAGUES,
  ...INTERNATIONAL_LEAGUE_FILTERS,
] as const

export function isInternationalLeagueCode(code: string | null | undefined): code is InternationalLeagueCode {
  if (!code) return false
  return code === INTERNATIONAL_LEAGUE_CODE || INTERNATIONAL_LEAGUE_FILTERS.some((league) => league.code === code)
}

export function getTeamLeagueLabel(code: string | null | undefined): string {
  if (!code) return '-'
  if (code === INTERNATIONAL_LEAGUE_CODE) return '국제전'
  const internationalLabel = INTERNATIONAL_LEAGUE_FILTERS.find((league) => league.code === code)?.label
  if (internationalLabel) return internationalLabel
  return TEAM_LEAGUES.find((league) => league.code === code)?.label ?? code
}
