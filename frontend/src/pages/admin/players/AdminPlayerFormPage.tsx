// 선수 등록/수정 폼 페이지
import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { playerFormSchema } from '../../../types/adminForms'
import type { PlayerFormValues } from '../../../types/adminForms'
import { useAdminPlayer, useAdminCreatePlayer, useAdminUpdatePlayer } from '../../../hooks/useAdminPlayers'
import { useAdminTeamList } from '../../../hooks/useAdminTeams'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

export function AdminPlayerFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const isEditMode = !!id
  const playerId = isEditMode ? Number(id) : 0

  const { data: existingPlayer } = useAdminPlayer(playerId)
  const { data: teams = [] } = useAdminTeamList()

  const createMutation = useAdminCreatePlayer()
  const updateMutation = useAdminUpdatePlayer()
  const isPending = createMutation.isPending || updateMutation.isPending
  const mutationError = createMutation.error || updateMutation.error

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PlayerFormValues>({
    resolver: zodResolver(playerFormSchema),
  })

  // 수정 모드: 기존 선수 정보로 초기화
  useEffect(() => {
    if (isEditMode && existingPlayer) {
      reset({
        inGameName: existingPlayer.inGameName,
        realName: existingPlayer.realName ?? '',
        role: existingPlayer.role ?? '',
        nationality: existingPlayer.nationality ?? '',
        profileImageUrl: existingPlayer.profileImageUrl ?? '',
        teamId: existingPlayer.teamId ?? null,
      })
    }
  }, [existingPlayer, isEditMode, reset])

  function onSubmit(data: PlayerFormValues) {
    if (isEditMode) {
      updateMutation.mutate(
        { id: playerId, data },
        { onSuccess: () => navigate('/admin/players') },
      )
    } else {
      createMutation.mutate(data, {
        onSuccess: () => navigate('/admin/players'),
      })
    }
  }

  const errorMessage =
    mutationError instanceof ApiError ? mutationError.message : mutationError?.message

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-6 text-xl font-bold text-gray-900">
        {isEditMode ? '선수 수정' : '선수 등록'}
      </h1>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <Field label="닉네임 *" error={errors.inGameName?.message}>
          <Input {...register('inGameName')} placeholder="게임 내 닉네임" />
        </Field>

        <Field label="실명" error={errors.realName?.message}>
          <Input {...register('realName')} placeholder="실명 (선택)" />
        </Field>

        <Field label="역할" error={errors.role?.message}>
          <Input {...register('role')} placeholder="예: Top, Jungle, Mid, Bot, Support" />
        </Field>

        <Field label="국적" error={errors.nationality?.message}>
          <Input {...register('nationality')} placeholder="예: 한국, 중국" />
        </Field>

        <Field label="프로필 이미지 URL" error={errors.profileImageUrl?.message}>
          <Input {...register('profileImageUrl')} type="url" placeholder="https://..." />
        </Field>

        <Field label="소속 팀" error={errors.teamId?.message}>
          <select
            {...register('teamId', { setValueAs: (v) => (v === '' ? null : Number(v)) })}
            className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm"
          >
            <option value="">미소속 (free agent)</option>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </Field>

        {errorMessage && <p className="text-sm text-red-600">{errorMessage}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={() => navigate('/admin/players')} disabled={isPending}>
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
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium text-gray-700">{label}</label>
      {children}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  )
}
