import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Footer } from './Footer'
import { MobileTabBar } from './MobileTabBar'

// 전체 페이지 공통 레이아웃 — Header + 본문 + Footer, 모바일은 하단 탭바 높이만큼 여백
export function RootLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-background pb-16 text-foreground md:pb-0">
      <Header />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:py-10">
        <Outlet />
      </main>
      <Footer />
      <MobileTabBar />
    </div>
  )
}
