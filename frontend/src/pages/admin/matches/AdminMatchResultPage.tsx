import { useEffect, useState, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { matchResultSchema } from '../../../types/adminForms'
import type { MatchResultFormValues } from '../../../types/adminForms'
import { useAdminMatch, useCreateMatchResult, useUpdateMatchResult } from '../../../hooks/useAdminMatches'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

function toDateTimeLocal(iso: string): string {
  return new Date(iso).toISOString().slice(0, 16)
}

export function AdminMatchResultPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const matchId = Number(id)
  const [formError, setFormError] = useState('')

  const { data: match, isLoading } = useAdminMatch(matchId)
  const hasResult = !!match?.result

  const createMutation = useCreateMatchResult()
  const updateMutation = useUpdateMatchResult()
  const isPending = createMutation.isPending || updateMutation.isPending
  const mutationError = createMutation.error || updateMutation.error

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MatchResultFormValues>({
    resolver: zodResolver(matchResultSchema),
    defaultValues: { scoreTeamA: 0, scoreTeamB: 0, vodUrl: '', notes: '' },
  })

  useEffect(() => {
    if (match?.result) {
      reset({
        winnerTeamId: match.result.winnerTeamId ?? undefined,
        scoreTeamA: match.result.scoreTeamA,
        scoreTeamB: match.result.scoreTeamB,
        playedAt: toDateTimeLocal(match.result.playedAt),
        vodUrl: match.result.vodUrl ?? '',
        notes: '',
      })
    }
  }, [match, reset])

  function onSubmit(data: MatchResultFormValues) {
    setFormError('')
    const mutation = hasResult ? updateMutation : createMutation
    mutation.mutate(
      { matchId, data },
      { onSuccess: () => navigate('/admin/matches') },
    )
  }

  function onInvalid() {
    setFormError('입력값을 다시 확인해주세요.')
  }

  if (isLoading) {
    return <div className="text-sm text-muted-foreground">불러오는 중...</div>
  }

  const errorMessage =
    formError || (mutationError instanceof ApiError ? mutationError.message : mutationError?.message)

  const teamOptions = match
    ? [
        { value: match.teamA.id, label: match.teamA.name },
        { value: match.teamB.id, label: match.teamB.name },
      ]
    : []

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-2 text-xl font-bold text-foreground">
        경기 결과 {hasResult ? '수정' : '입력'}
      </h1>
      {match && (
        <p className="mb-6 text-sm text-muted-foreground">
          {match.teamA.name} vs {match.teamB.name} · {match.tournamentName}
        </p>
      )}

      <form noValidate onSubmit={handleSubmit(onSubmit, onInvalid)} className="flex flex-col gap-4">
        <Field label="승리 팀" error={errors.winnerTeamId?.message}>
          <select
            {...register('winnerTeamId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}
            className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm"
          >
            <option value="">승리 팀 선택</option>
            {teamOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label={`${match?.teamA.name ?? '팀 A'} 점수`} error={errors.scoreTeamA?.message}>
            <Input
              {...register('scoreTeamA', { valueAsNumber: true })}
              type="number"
              min={0}
              placeholder="0"
            />
          </Field>
          <Field label={`${match?.teamB.name ?? '팀 B'} 점수`} error={errors.scoreTeamB?.message}>
            <Input
              {...register('scoreTeamB', { valueAsNumber: true })}
              type="number"
              min={0}
              placeholder="0"
            />
          </Field>
        </div>

        <Field label="경기 시각" error={errors.playedAt?.message}>
          <Input {...register('playedAt')} type="datetime-local" />
        </Field>

        <Field label="VOD URL (선택)" error={errors.vodUrl?.message}>
          <Input {...register('vodUrl')} type="url" placeholder="https://..." />
        </Field>

        <Field label="비고 (선택)" error={errors.notes?.message}>
          <textarea
            {...register('notes')}
            rows={3}
            className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            placeholder="경기 관련 메모"
          />
        </Field>

        {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}

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

function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-sm font-medium text-foreground">{label}</label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}
