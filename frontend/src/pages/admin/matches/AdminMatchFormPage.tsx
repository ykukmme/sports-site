import { useEffect, useState, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { matchCreateSchema, matchUpdateSchema } from '../../../types/adminForms'
import type { MatchCreateFormValues, MatchUpdateFormValues } from '../../../types/adminForms'
import { useAdminMatch, useAdminCreateMatch, useAdminUpdateMatch } from '../../../hooks/useAdminMatches'
import { fetchGamesForAdmin, fetchAdminTeams } from '../../../api/admin'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

function toDateTimeLocal(iso: string): string {
  return new Date(iso).toISOString().slice(0, 16)
}

export function AdminMatchFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const isEditMode = !!id
  const matchId = isEditMode ? Number(id) : 0
  const [formError, setFormError] = useState('')

  const { data: existingMatch } = useAdminMatch(matchId)

  const { data: games = [] } = useQuery({
    queryKey: ['admin', 'games'],
    queryFn: fetchGamesForAdmin,
    staleTime: Infinity,
  })
  const { data: teams = [] } = useQuery({
    queryKey: ['admin', 'teams'],
    queryFn: fetchAdminTeams,
    staleTime: 60_000,
  })

  const createMutation = useAdminCreateMatch()
  const updateMutation = useAdminUpdateMatch()
  const isPending = createMutation.isPending || updateMutation.isPending
  const mutationError = createMutation.error || updateMutation.error

  const createForm = useForm<MatchCreateFormValues>({
    resolver: zodResolver(matchCreateSchema),
  })
  const updateForm = useForm<MatchUpdateFormValues>({
    resolver: zodResolver(matchUpdateSchema),
  })

  useEffect(() => {
    if (isEditMode && existingMatch) {
      updateForm.reset({
        tournamentName: existingMatch.tournamentName,
        stage: existingMatch.stage ?? '',
        scheduledAt: toDateTimeLocal(existingMatch.scheduledAt),
        status: existingMatch.status,
      })
    }
  }, [existingMatch, isEditMode, updateForm])

  function handleCreateSubmit(data: MatchCreateFormValues) {
    setFormError('')
    createMutation.mutate(data, {
      onSuccess: () => navigate('/admin/matches'),
    })
  }

  function handleUpdateSubmit(data: MatchUpdateFormValues) {
    setFormError('')
    updateMutation.mutate(
      { id: matchId, data },
      { onSuccess: () => navigate('/admin/matches') },
    )
  }

  function onInvalid() {
    setFormError('입력값을 다시 확인해주세요.')
  }

  const errorMessage =
    formError || (mutationError instanceof ApiError ? mutationError.message : mutationError?.message)

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-6 text-xl font-bold text-foreground">
        {isEditMode ? '경기 수정' : '경기 등록'}
      </h1>

      {isEditMode ? (
        <form
          noValidate
          onSubmit={updateForm.handleSubmit(handleUpdateSubmit, onInvalid)}
          className="flex flex-col gap-4"
        >
          <Field label="대회명" error={updateForm.formState.errors.tournamentName?.message}>
            <Input {...updateForm.register('tournamentName')} placeholder="대회명" />
          </Field>
          <Field label="단계" error={updateForm.formState.errors.stage?.message}>
            <Input {...updateForm.register('stage')} placeholder="예: 4강, 결승" />
          </Field>
          <Field label="예정 시각" error={updateForm.formState.errors.scheduledAt?.message}>
            <Input {...updateForm.register('scheduledAt')} type="datetime-local" />
          </Field>
          <Field label="상태" error={updateForm.formState.errors.status?.message}>
            <select
              {...updateForm.register('status')}
              className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
            >
              <option value="SCHEDULED">예정</option>
              <option value="ONGOING">진행 중</option>
              <option value="COMPLETED">완료</option>
              <option value="CANCELLED">취소</option>
            </select>
          </Field>

          {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}
          <FormActions onCancel={() => navigate('/admin/matches')} isPending={isPending} />
        </form>
      ) : (
        <form
          noValidate
          onSubmit={createForm.handleSubmit(handleCreateSubmit, onInvalid)}
          className="flex flex-col gap-4"
        >
          <Field label="종목 *" error={createForm.formState.errors.gameId?.message}>
            <select
              {...createForm.register('gameId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}
              className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
            >
              <option value="">종목 선택</option>
              {games.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </Field>
          <Field label="팀 A *" error={createForm.formState.errors.teamAId?.message}>
            <select
              {...createForm.register('teamAId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}
              className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
            >
              <option value="">팀 A 선택</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </Field>
          <Field label="팀 B *" error={createForm.formState.errors.teamBId?.message}>
            <select
              {...createForm.register('teamBId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}
              className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
            >
              <option value="">팀 B 선택</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </Field>
          <Field label="대회명 *" error={createForm.formState.errors.tournamentName?.message}>
            <Input {...createForm.register('tournamentName')} placeholder="대회명" />
          </Field>
          <Field label="단계" error={createForm.formState.errors.stage?.message}>
            <Input {...createForm.register('stage')} placeholder="예: 4강, 결승" />
          </Field>
          <Field label="예정 시각 *" error={createForm.formState.errors.scheduledAt?.message}>
            <Input {...createForm.register('scheduledAt')} type="datetime-local" />
          </Field>

          {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}
          <FormActions onCancel={() => navigate('/admin/matches')} isPending={isPending} />
        </form>
      )}
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

function FormActions({ onCancel, isPending }: { onCancel: () => void; isPending: boolean }) {
  return (
    <div className="flex justify-end gap-2 pt-2">
      <Button type="button" variant="outline" onClick={onCancel} disabled={isPending}>
        취소
      </Button>
      <Button type="submit" disabled={isPending}>
        {isPending ? '저장 중...' : '저장'}
      </Button>
    </div>
  )
}
