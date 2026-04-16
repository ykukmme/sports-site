// 경기 결과 등록/수정 페이지
import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { matchResultSchema } from '../../../types/adminForms'
import type { MatchResultFormValues } from '../../../types/adminForms'
import { useAdminMatch } from '../../../hooks/useAdminMatches'
import { useCreateMatchResult, useUpdateMatchResult } from '../../../hooks/useAdminMatches'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

// datetime-local 포맷 변환
function toDateTimeLocal(iso: string): string {
  return new Date(iso).toISOString().slice(0, 16)
}

export function AdminMatchResultPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const matchId = Number(id)

  const { data: match, isLoading } = useAdminMatch(matchId)
  const hasResult = !!match?.result

  const createMutation = useCreateMatchResult()
  const updateMutation = useUpdateMatchResult()
  const isPending = createMutation.isPending || updateMutation.isPending
  const mutationError = createMutation.error || updateMutation.error

  const { register, handleSubmit, reset, formState: { errors } } = useForm<MatchResultFormValues>({
    resolver: zodResolver(matchResultSchema),
    defaultValues: { scoreTeamA: 0, scoreTeamB: 0 },
  })

  // 기존 결과가 있으면 폼 초기화
  useEffect(() => {
    if (match?.result) {
      reset({
        winnerTeamId: match.result.winnerTeamId ?? undefined,
        scoreTeamA: match.result.scoreTeamA,
        scoreTeamB: match.result.scoreTeamB,
        playedAt: toDateTimeLocal(match.result.playedAt),
        vodUrl: match.result.vodUrl ?? '',
      })
    }
  }, [match, reset])

  function onSubmit(data: MatchResultFormValues) {
    const mutation = hasResult ? updateMutation : createMutation
    mutation.mutate(
      { matchId, data },
      { onSuccess: () => navigate('/admin/matches') },
    )
  }

  if (isLoading) {
    return <div className="text-sm text-gray-500">불러오는 중...</div>
  }

  const errorMessage =
    mutationError instanceof ApiError ? mutationError.message : mutationError?.message

  // 팀 선택지 — 팀 A / 팀 B
  const teamOptions = match
    ? [
        { value: match.teamA.id, label: match.teamA.name },
        { value: match.teamB.id, label: match.teamB.name },
      ]
    : []

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-2 text-xl font-bold text-gray-900">
        경기 결과 {hasResult ? '수정' : '입력'}
      </h1>
      {match && (
        <p className="mb-6 text-sm text-gray-500">
          {match.teamA.name} vs {match.teamB.name} — {match.tournamentName}
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700">승리 팀</label>
          <select
            {...register('winnerTeamId', { valueAsNumber: true })}
            className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm"
            required
          >
            <option value="">승리 팀 선택</option>
            {teamOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
          {errors.winnerTeamId && (
            <p className="text-xs text-red-600">{errors.winnerTeamId.message}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">
              {match?.teamA.name ?? '팀 A'} 점수
            </label>
            <Input
              {...register('scoreTeamA', { valueAsNumber: true })}
              type="number"
              min={0}
              placeholder="0"
            />
            {errors.scoreTeamA && (
              <p className="text-xs text-red-600">{errors.scoreTeamA.message}</p>
            )}
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700">
              {match?.teamB.name ?? '팀 B'} 점수
            </label>
            <Input
              {...register('scoreTeamB', { valueAsNumber: true })}
              type="number"
              min={0}
              placeholder="0"
            />
            {errors.scoreTeamB && (
              <p className="text-xs text-red-600">{errors.scoreTeamB.message}</p>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700">경기 시각</label>
          <Input {...register('playedAt')} type="datetime-local" />
          {errors.playedAt && (
            <p className="text-xs text-red-600">{errors.playedAt.message}</p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700">VOD URL (선택)</label>
          <Input {...register('vodUrl')} type="url" placeholder="https://..." />
          {errors.vodUrl && (
            <p className="text-xs text-red-600">{errors.vodUrl.message}</p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700">비고 (선택)</label>
          <textarea
            {...register('notes')}
            rows={3}
            className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            placeholder="경기 관련 메모"
          />
          {errors.notes && (
            <p className="text-xs text-red-600">{errors.notes.message}</p>
          )}
        </div>

        {errorMessage && <p className="text-sm text-red-600">{errorMessage}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={() => navigate('/admin/matches')} disabled={isPending}>
            취소
          </Button>
          <Button type="submit" disabled={isPending}>
            {isPending ? '저장 중...' : '저장'}
          </Button>
        </div>
      </form>
    </div>
  )
}
