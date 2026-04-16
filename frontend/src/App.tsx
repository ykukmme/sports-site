import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from './context/ThemeContext'
import { TeamThemeProvider } from './context/TeamThemeContext'
import { RootLayout } from './components/layout/RootLayout'
import { ErrorMessage } from './components/common/ErrorMessage'
import { HomePage } from './pages/HomePage'
import { UpcomingMatchesPage } from './pages/UpcomingMatchesPage'
import { MatchResultsPage } from './pages/MatchResultsPage'
import { TeamsPage } from './pages/TeamsPage'
import { TeamDetailPage } from './pages/TeamDetailPage'
import { PlayerDetailPage } from './pages/PlayerDetailPage'
// 어드민 라우팅
import { AdminRoute } from './components/admin/AdminRoute'
import { AdminLayout } from './components/admin/AdminLayout'
import { AdminLoginPage } from './pages/admin/AdminLoginPage'
import { AdminMatchListPage } from './pages/admin/matches/AdminMatchListPage'
import { AdminMatchFormPage } from './pages/admin/matches/AdminMatchFormPage'
import { AdminMatchResultPage } from './pages/admin/matches/AdminMatchResultPage'
import { AdminTeamListPage } from './pages/admin/teams/AdminTeamListPage'
import { AdminTeamFormPage } from './pages/admin/teams/AdminTeamFormPage'
import { AdminPlayerListPage } from './pages/admin/players/AdminPlayerListPage'
import { AdminPlayerFormPage } from './pages/admin/players/AdminPlayerFormPage'

// React Query 클라이언트 — 전역 기본 staleTime 60초
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
    },
  },
})

// 앱 진입점 — 라우팅 및 전역 레이아웃
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
      <TeamThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootLayout />}>
            <Route index element={<HomePage />} />
            <Route path="matches/upcoming" element={<UpcomingMatchesPage />} />
            <Route path="matches/results" element={<MatchResultsPage />} />
            <Route path="teams" element={<TeamsPage />} />
            <Route path="teams/:id" element={<TeamDetailPage />} />
            <Route path="players/:id" element={<PlayerDetailPage />} />
            <Route path="*" element={<ErrorMessage message="페이지를 찾을 수 없습니다." />} />
          </Route>

          {/* 어드민 로그인 — 인증 불필요 */}
          <Route path="/admin/login" element={<AdminLoginPage />} />

          {/* 어드민 보호 라우트 — AdminRoute에서 쿠키 유효성 확인 */}
          <Route element={<AdminRoute />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<Navigate to="/admin/matches" replace />} />
              <Route path="/admin/matches" element={<AdminMatchListPage />} />
              <Route path="/admin/matches/new" element={<AdminMatchFormPage />} />
              <Route path="/admin/matches/:id/edit" element={<AdminMatchFormPage />} />
              <Route path="/admin/matches/:id/result" element={<AdminMatchResultPage />} />
              <Route path="/admin/teams" element={<AdminTeamListPage />} />
              <Route path="/admin/teams/new" element={<AdminTeamFormPage />} />
              <Route path="/admin/teams/:id/edit" element={<AdminTeamFormPage />} />
              <Route path="/admin/players" element={<AdminPlayerListPage />} />
              <Route path="/admin/players/new" element={<AdminPlayerFormPage />} />
              <Route path="/admin/players/:id/edit" element={<AdminPlayerFormPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
      </TeamThemeProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export default App
