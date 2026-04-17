// 에러 메시지 표시 컴포넌트
interface ErrorMessageProps {
  message: string
}

export function ErrorMessage({ message }: ErrorMessageProps) {
  // 영문 네이티브 에러 메시지가 노출되지 않도록 fallback 적용
  const displayMessage = message || '오류가 발생했습니다.'
  return (
    <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
      {displayMessage}
    </div>
  )
}
