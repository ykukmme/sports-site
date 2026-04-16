// 어드민 로그인 페이지
import { useEffect, useState } from 'react'
import { useAdminAuth } from '../../hooks/useAdminAuth'
import { Input } from '../../components/ui/input'
import { Button } from '../../components/ui/button'
import { ApiError } from '../../api/client'

export function AdminLoginPage() {
  const { login, isLoggingIn, isAuthenticated } = useAdminAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (isAuthenticated) {
      window.location.href = '/admin/matches'
    }
  }, [isAuthenticated])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    login(
      { username, password },
      {
        onError: (err) => {
          setError(err instanceof ApiError ? err.message : '로그인에 실패했습니다.')
        },
      },
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted">
      <div className="w-80 rounded-xl bg-card p-8 shadow-card-subtle ring-1 ring-border">
        <h1 className="mb-6 text-xl font-bold text-foreground">어드민 로그인</h1>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label htmlFor="username" className="text-sm font-medium text-foreground">
              아이디
            </label>
            <Input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoComplete="username"
              placeholder="관리자 아이디"
            />
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="password" className="text-sm font-medium text-foreground">
              비밀번호
            </label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              placeholder="비밀번호"
            />
          </div>

          {/* 로그인 오류 메시지 */}
          {error && (
            <p className="text-sm text-destructive">{error}</p>
          )}

          <Button type="submit" disabled={isLoggingIn} className="mt-2">
            {isLoggingIn ? '로그인 중...' : '로그인'}
          </Button>
        </form>
      </div>
    </div>
  )
}
