import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'

// 테마 모드 — system은 OS 설정 자동 감지
type ThemeMode = 'light' | 'dark' | 'system'

interface ThemeContextValue {
  theme: ThemeMode
  resolvedTheme: 'light' | 'dark'
  setTheme: (t: ThemeMode) => void
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

// OS 다크 모드 감지 유틸
function getSystemDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 저장된 테마 불러오기 (없으면 system)
function getSavedTheme(): ThemeMode {
  const saved = localStorage.getItem('theme')
  if (saved === 'light' || saved === 'dark' || saved === 'system') return saved
  return 'system'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(getSavedTheme)
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>(
    () => (theme === 'system' ? (getSystemDark() ? 'dark' : 'light') : theme)
  )

  // resolvedTheme 변경 시 html 클래스 동기 반영
  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')
  }, [resolvedTheme])

  // system 모드일 때 OS 설정 변경 감지
  useEffect(() => {
    if (theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = (e: MediaQueryListEvent) => {
      setResolvedTheme(e.matches ? 'dark' : 'light')
    }
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [theme])

  const setTheme = (t: ThemeMode) => {
    setThemeState(t)
    localStorage.setItem('theme', t)
    const resolved = t === 'system' ? (getSystemDark() ? 'dark' : 'light') : t
    setResolvedTheme(resolved)
  }

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

// useTheme 훅 — ThemeProvider 외부에서 사용 시 에러 throw
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme은 ThemeProvider 안에서만 사용할 수 있습니다.')
  return ctx
}
