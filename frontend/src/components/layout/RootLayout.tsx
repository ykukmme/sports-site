import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Footer } from './Footer'

// 전체 페이지 공통 레이아웃 — Header + 본문 + Footer
export function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="container mx-auto px-4 py-12 flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
