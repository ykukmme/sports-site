import { useEffect, useState, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { playerFormSchema } from '../../../types/adminForms'
import type { PlayerFormValues } from '../../../types/adminForms'
import { useAdminPlayer, useAdminCreatePlayer, useAdminUpdatePlayer } from '../../../hooks/useAdminPlayers'
import { useAdminTeamList } from '../../../hooks/useAdminTeams'
import { uploadPlayerProfileImage } from '../../../api/admin'
import { Input } from '../../../components/ui/input'
import { Button } from '../../../components/ui/button'
import { ApiError } from '../../../api/client'

const ROLE_OPTIONS = ['TOP', 'JGL', 'MID', 'BOT', 'SPT', 'HEAD COACH', 'COACH'] as const
type RosterRole = (typeof ROLE_OPTIONS)[number]

function toRosterRole(value: string | null | undefined): RosterRole | '' {
  return ROLE_OPTIONS.includes(value as RosterRole) ? (value as RosterRole) : ''
}

export function AdminPlayerFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const isEditMode = !!id
  const playerId = isEditMode ? Number(id) : 0
  const [formError, setFormError] = useState('')
  const [isImageUploading, setIsImageUploading] = useState(false)

  const { data: existingPlayer } = useAdminPlayer(playerId)
  const { data: teams = [] } = useAdminTeamList()

  const createMutation = useAdminCreatePlayer()
  const updateMutation = useAdminUpdatePlayer()
  const isPending = createMutation.isPending || updateMutation.isPending || isImageUploading
  const mutationError = createMutation.error || updateMutation.error

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<PlayerFormValues>({
    resolver: zodResolver(playerFormSchema),
    defaultValues: {
      inGameName: '',
      realName: '',
      role: '',
      nationality: '',
      profileImageUrl: '',
      teamId: null,
    },
  })

  useEffect(() => {
    if (isEditMode && existingPlayer) {
      reset({
        inGameName: existingPlayer.inGameName,
        realName: existingPlayer.realName ?? '',
        role: toRosterRole(existingPlayer.role),
        nationality: existingPlayer.nationality ?? '',
        profileImageUrl: existingPlayer.profileImageUrl ?? '',
        teamId: existingPlayer.teamId ?? null,
      })
    }
  }, [existingPlayer, isEditMode, reset])

  function onSubmit(data: PlayerFormValues) {
    setFormError('')
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

  function onInvalid() {
    setFormError('입력값을 다시 확인해주세요.')
  }

  const profileImageUrl = watch('profileImageUrl')
  const errorMessage =
    formError || (mutationError instanceof ApiError ? mutationError.message : mutationError?.message)

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-6 text-xl font-bold text-foreground">
        {isEditMode ? '로스터 수정' : '로스터 등록'}
      </h1>

      <form noValidate onSubmit={handleSubmit(onSubmit, onInvalid)} className="flex flex-col gap-4">
        <Field label="닉네임 *" error={errors.inGameName?.message}>
          <Input {...register('inGameName')} placeholder="게임 내 닉네임" />
        </Field>

        <Field label="본명" error={errors.realName?.message}>
          <Input {...register('realName')} placeholder="본명 (선택)" />
        </Field>

        <Field label="역할" error={errors.role?.message}>
          <select
            {...register('role')}
            className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
          >
            <option value="">역할 선택</option>
            {ROLE_OPTIONS.map((role) => (
              <option key={role} value={role}>{role}</option>
            ))}
          </select>
        </Field>

        <Field label="국적" error={errors.nationality?.message}>
          <Input {...register('nationality')} placeholder="예: 한국, 중국" />
        </Field>

        <Field label="프로필 이미지 URL" error={errors.profileImageUrl?.message}>
          <Input {...register('profileImageUrl')} type="url" placeholder="https://..." />
        </Field>

        <Field label="프로필 이미지 업로드">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                disabled={isImageUploading}
                onChange={async (event) => {
                  const file = event.target.files?.[0]
                  if (!file) return
                  setFormError('')
                  setIsImageUploading(true)
                  try {
                    const uploadedImageUrl = await uploadPlayerProfileImage(file)
                    setValue('profileImageUrl', uploadedImageUrl, { shouldDirty: true, shouldValidate: true })
                  } catch (error) {
                    setFormError(error instanceof ApiError ? error.message : '프로필 이미지 업로드에 실패했습니다.')
                  } finally {
                    setIsImageUploading(false)
                    event.target.value = ''
                  }
                }}
                className="block w-full text-sm text-muted-foreground file:mr-3 file:rounded-md file:border file:border-input file:bg-card file:px-3 file:py-1.5 file:text-sm file:text-foreground"
              />
              {isImageUploading && <span className="text-xs text-muted-foreground">업로드 중...</span>}
            </div>
            {profileImageUrl && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <img src={profileImageUrl} alt="프로필 이미지 미리보기" className="size-12 rounded-md border border-border object-cover" />
                <span>로스터를 저장하면 이 이미지가 연결됩니다.</span>
              </div>
            )}
          </div>
        </Field>

        <Field label="소속 팀" error={errors.teamId?.message}>
          <select
            {...register('teamId', { setValueAs: (v) => (v === '' ? null : Number(v)) })}
            className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm text-foreground"
          >
            <option value="">미소속 (free agent)</option>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </Field>

        {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}

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
