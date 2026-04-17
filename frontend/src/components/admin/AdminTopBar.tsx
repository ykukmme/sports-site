// 어드민 상단 바 — 로그아웃 버튼
import { Button } from '../ui/button'

interface AdminTopBarProps {
  onLogout: () => void
}

export function AdminTopBar({ onLogout }: AdminTopBarProps) {
  return (
    <header className="flex h-14 shrink-0 items-center justify-end border-b border-border bg-background px-6">
      <Button variant="outline" size="sm" onClick={onLogout}>
        로그아웃
      </Button>
    </header>
  )
}
