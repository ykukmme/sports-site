// 팀 등록/수정 폼 페이지
import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { teamFormSchema } from '../../../types/adminForms'
import type { TeamFormValues } from '../../../types/adminForms'
import { useAdminTeam, useAdminCreateTeam, useAdminUpdateTeam } from '../../../hooks/useAdminTeams'
import { fetchGamesForAdmin } from '../../../api/admin'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

export function AdminTeamFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const isEditMode = !!id
  const teamId = isEditMode ? Number(id) : 0

  const { data: existingTeam } = useAdminTeam(teamId)
  const { data: games = [] } = useQuery({
    queryKey: ['admin', 'games'],
    queryFn: fetchGamesForAdmin,
    staleTime: Infinity,
  })

  const createMutation = useAdminCreateTeam()
  const updateMutation = useAdminUpdateTeam()
  const isPending = createMutation.isPending || updateMutation.isPending
  const mutationError = createMutation.error || updateMutation.error

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<TeamFormValues>({
    resolver: zodResolver(teamFormSchema),
    defaultValues: { primaryColor: '', secondaryColor: '' },
  })

  // 수정 모드: 기존 팀 정보로 초기화
  useEffect(() => {
    if (isEditMode && existingTeam) {
      reset({
        name: existingTeam.name,
        shortName: existingTeam.shortName ?? '',
        region: existingTeam.region ?? '',
        logoUrl: existingTeam.logoUrl ?? '',
        gameId: existingTeam.gameId,
        primaryColor: existingTeam.primaryColor ?? '',
        secondaryColor: existingTeam.secondaryColor ?? '',
      })
    }
  }, [existingTeam, isEditMode, reset])

  function onSubmit(data: TeamFormValues) {
    if (isEditMode) {
      updateMutation.mutate(
        { id: teamId, data },
        { onSuccess: () => navigate('/admin/teams') },
      )
    } else {
      createMutation.mutate(data, {
        onSuccess: () => navigate('/admin/teams'),
      })
    }
  }

  const primaryColor = watch('primaryColor')
  const secondaryColor = watch('secondaryColor')

  const errorMessage =
    mutationError instanceof ApiError ? mutationError.message : mutationError?.message

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-6 text-xl font-bold text-foreground">
        {isEditMode ? '팀 수정' : '팀 등록'}
      </h1>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <Field label="팀명 *" error={errors.name?.message}>
          <Input {...register('name')} placeholder="팀명" />
        </Field>

        <Field label="약칭" error={errors.shortName?.message}>
          <Input {...register('shortName')} placeholder="예: T1, GEN" />
        </Field>

        <Field label="지역" error={errors.region?.message}>
          <Input {...register('region')} placeholder="예: 한국, 중국" />
        </Field>

        <Field label="로고 URL" error={errors.logoUrl?.message}>
          <Input {...register('logoUrl')} type="url" placeholder="https://..." />
        </Field>

        <Field label="종목 *" error={errors.gameId?.message}>
          <select
            {...register('gameId', { valueAsNumber: true })}
            className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm"
          >
            <option value="">종목 선택</option>
            {games.map((g) => (
              <option key={g.id} value={g.id}>{g.name}</option>
            ))}
          </select>
        </Field>

        {/* 팀 테마 색상 — 컬러 피커 + 텍스트 입력 동기화 */}
        <Field label="팀 색상 (primaryColor)" error={errors.primaryColor?.message}>
          <div className="flex items-center gap-2">
            <input
              type="color"
              value={primaryColor || '#000000'}
              onChange={(e) => setValue('primaryColor', e.target.value)}
              className="h-8 w-10 cursor-pointer rounded border border-input p-0.5"
            />
            <Input
              {...register('primaryColor')}
              placeholder="#RRGGBB"
              className="font-mono"
              maxLength={7}
            />
          </div>
        </Field>

        <Field label="보조 색상 (secondaryColor)" error={errors.secondaryColor?.message}>
          <div className="flex items-center gap-2">
            <input
              type="color"
              value={secondaryColor || '#000000'}
              onChange={(e) => setValue('secondaryColor', e.target.value)}
              className="h-8 w-10 cursor-pointer rounded border border-input p-0.5"
            />
            <Input
              {...register('secondaryColor')}
              placeholder="#RRGGBB"
              className="font-mono"
              maxLength={7}
            />
          </div>
        </Field>

        {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={() => navigate('/admin/teams')} disabled={isPending}>
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
    <div className="flex flex-col gap-2">
      <label className="text-sm font-medium text-foreground">{label}</label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}
