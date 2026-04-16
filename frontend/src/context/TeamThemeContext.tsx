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

// CSS variable 오버라이드 — html 엘리먼트에 직접 주입
function applyTeamColor(color: string) {
  document.documentElement.style.setProperty('--primary', color)
  document.documentElement.style.setProperty('--ring', color)
}

function clearTeamColor() {
  document.documentElement.style.removeProperty('--primary')
  document.documentElement.style.removeProperty('--ring')
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
