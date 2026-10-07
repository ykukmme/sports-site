import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { fetchTeamById } from '../api/teams'
import type { TeamResponse } from '../types/domain'

interface TeamThemeContextValue {
  activeTeamId: number | null
  activeTeam: TeamResponse | null
  setTeamTheme: (team: TeamResponse | null) => void
}

const TeamThemeContext = createContext<TeamThemeContextValue | undefined>(undefined)

// 팀 컬러 위 글자색 — 밝기(상대 휘도)에 따라 흰색/검정 중 대비가 큰 쪽 선택
function getReadableForeground(color: string): string | null {
  const hex = color.trim().replace('#', '')
  if (!/^([0-9a-f]{3}|[0-9a-f]{6})$/i.test(hex)) return null
  const full = hex.length === 3 ? hex.split('').map((c) => c + c).join('') : hex
  const [r, g, b] = [0, 2, 4].map((i) => {
    const v = parseInt(full.slice(i, i + 2), 16) / 255
    return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4
  })
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
  return (luminance + 0.05) / 0.05 > 1.05 / (luminance + 0.05) ? '#050507' : '#ffffff'
}

// CSS variable 오버라이드 — html 엘리먼트에 직접 주입
function applyTeamColor(color: string) {
  document.documentElement.style.setProperty('--primary', color)
  document.documentElement.style.setProperty('--ring', color)
  const foreground = getReadableForeground(color)
  if (foreground) document.documentElement.style.setProperty('--primary-foreground', foreground)
  else document.documentElement.style.removeProperty('--primary-foreground')
}

function clearTeamColor() {
  document.documentElement.style.removeProperty('--primary')
  document.documentElement.style.removeProperty('--ring')
  document.documentElement.style.removeProperty('--primary-foreground')
}

export function TeamThemeProvider({ children }: { children: ReactNode }) {
  const [activeTeam, setActiveTeam] = useState<TeamResponse | null>(null)

  // 초기화 — localStorage에 저장된 팀 ID로 색상 복원
  useEffect(() => {
    const saved = localStorage.getItem('fan-team-id')
    if (!saved) return

    const teamId = parseInt(saved, 10)
    if (isNaN(teamId)) {
      localStorage.removeItem('fan-team-id')
      return
    }

    fetchTeamById(teamId)
      .then((team) => {
        setActiveTeam(team)
        if (team.primaryColor) applyTeamColor(team.primaryColor)
      })
      .catch(() => {
        // 팀이 삭제됐거나 서버 오류 — 저장된 설정 제거
        localStorage.removeItem('fan-team-id')
      })
  }, [])

  const setTeamTheme = (team: TeamResponse | null) => {
    if (team === null) {
      setActiveTeam(null)
      localStorage.removeItem('fan-team-id')
      clearTeamColor()
    } else {
      setActiveTeam(team)
      localStorage.setItem('fan-team-id', team.id.toString())
      if (team.primaryColor) applyTeamColor(team.primaryColor)
    }
  }

  return (
    <TeamThemeContext.Provider
      value={{ activeTeamId: activeTeam?.id ?? null, activeTeam, setTeamTheme }}
    >
      {children}
    </TeamThemeContext.Provider>
  )
}

// useTeamTheme 훅 — TeamThemeProvider 외부에서 사용 시 에러 throw
export function useTeamTheme(): TeamThemeContextValue {
  const ctx = useContext(TeamThemeContext)
  if (!ctx) throw new Error('useTeamTheme은 TeamThemeProvider 안에서만 사용할 수 있습니다.')
  return ctx
}
