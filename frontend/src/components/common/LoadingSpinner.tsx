// 로딩 스피너 — 데이터 로딩 중 표시
interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
}

const sizeClass = {
  sm: 'w-4 h-4',
  md: 'w-8 h-8',
  lg: 'w-12 h-12',
}

export function LoadingSpinner({ size = 'md' }: LoadingSpinnerProps) {
  return (
    <div className="flex justify-center items-center py-8">
      <div
        className={`${sizeClass[size]} border-2 border-muted border-t-primary rounded-full animate-spin`}
        role="status"
        aria-label="로딩 중"
      />
    </div>
  )
}
