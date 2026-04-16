// 데이터 없음 상태 표시 컴포넌트
interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex justify-center items-center py-12 text-muted-foreground text-sm">
      {message}
    </div>
  )
}
