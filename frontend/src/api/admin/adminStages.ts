// 어드민 대회 단계 API — 목록 조회, 표시 방식 변경
import apiClient from '../client'
import type { ApiResponse, PageResponse } from '../../types/api'
import type {
  AdminStageResponse,
  BracketResponse,
  BracketSyncResponse,
  BracketOutcome,
  StageBackfillResponse,
  StageCoverageResponse,
  StageRegisterResponse,
  StageSourceSyncResponse,
  StageUpdateRequest,
} from '../../types/domain'

const EMPTY_PAGE: PageResponse<AdminStageResponse> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true,
}

// 단계 목록 (리그명 부분 검색, 페이지네이션)
export async function fetchAdminStages(league: string, page = 0): Promise<PageResponse<AdminStageResponse>> {
  const res = await apiClient.get<ApiResponse<PageResponse<AdminStageResponse>>>('/api/admin/stages', {
    params: { league: league || undefined, page, size: 50 },
  })
  return res.data.data ?? EMPTY_PAGE
}

// tournament id 로 단계 직접 등록 (전 경기 TBD 단계 포함)
export async function registerAdminStage(externalTournamentId: number): Promise<StageRegisterResponse> {
  const res = await apiClient.post<ApiResponse<StageRegisterResponse>>('/api/admin/stages/register', {
    externalTournamentId,
  })
  if (!res.data.data) throw new Error('단계 등록에 실패했습니다.')
  return res.data.data
}

// 기존 경기의 누락 단계 복구 (최대 100개)
export async function backfillAdminStages(externalTournamentIds: number[]): Promise<StageBackfillResponse> {
  const res = await apiClient.post<ApiResponse<StageBackfillResponse>>('/api/admin/stages/backfill', {
    externalTournamentIds,
  })
  if (!res.data.data) throw new Error('단계 복구에 실패했습니다.')
  return res.data.data
}

// 표시 방식 변경
export async function updateAdminStage(id: number, body: StageUpdateRequest): Promise<AdminStageResponse> {
  const res = await apiClient.patch<ApiResponse<AdminStageResponse>>(`/api/admin/stages/${id}`, body)
  if (!res.data.data) throw new Error('단계 설정 저장에 실패했습니다.')
  return res.data.data
}

// PandaScore 원본 경기 스냅샷 동기화 — 검증 실패 시 기존 스냅샷 유지
export async function syncStageSource(stageId: number): Promise<StageSourceSyncResponse> {
  const res = await apiClient.post<ApiResponse<StageSourceSyncResponse>>(`/api/admin/stages/${stageId}/source-sync`)
  if (!res.data.data) throw new Error('원본 경기 동기화에 실패했습니다.')
  return res.data.data
}

// 스위스 데이터 완전성 상세
export async function fetchStageCoverage(stageId: number): Promise<StageCoverageResponse> {
  const res = await apiClient.get<ApiResponse<StageCoverageResponse>>(`/api/admin/stages/${stageId}/coverage`)
  if (!res.data.data) throw new Error('데이터 완전성 정보를 불러오지 못했습니다.')
  return res.data.data
}

// 어드민 대진표 미리보기 (표시 방식과 무관)
export async function fetchAdminBracket(stageId: number): Promise<BracketResponse> {
  const res = await apiClient.get<ApiResponse<BracketResponse>>(`/api/admin/stages/${stageId}/bracket`)
  if (!res.data.data) throw new Error('대진표를 불러오지 못했습니다.')
  return res.data.data
}

// PandaScore 대진표 동기화
export async function syncAdminBracket(stageId: number): Promise<BracketSyncResponse> {
  const res = await apiClient.post<ApiResponse<BracketSyncResponse>>(`/api/admin/stages/${stageId}/bracket/sync`)
  if (!res.data.data) throw new Error('대진표 동기화에 실패했습니다.')
  return res.data.data
}

// 대진표 초기화 — 어드민 보정까지 모두 삭제
export async function resetAdminBracket(stageId: number): Promise<void> {
  await apiClient.delete(`/api/admin/stages/${stageId}/bracket`)
}

// 노드 배치 보정 — reset 이면 자동 배치로 복귀
export async function updateBracketNodeLayout(
  nodeId: number,
  body: { columnOverride: number | null; rowOverride: number | null; reset: boolean },
): Promise<void> {
  await apiClient.patch(`/api/admin/bracket-nodes/${nodeId}`, body)
}

// 팀 줄 표시 문구 — 빈 값이면 삭제
export async function updateBracketSlot(nodeId: number, slotIndex: number, label: string): Promise<void> {
  await apiClient.put(`/api/admin/bracket-nodes/${nodeId}/slots/${slotIndex}`, { label })
}

// 진출 간선 생성·수정 — (출처, 진출 유형) 기준, 팀 줄은 확인한 경우에만 지정
export async function upsertBracketEdge(body: {
  sourceNodeId: number
  outcome: BracketOutcome
  targetNodeId: number
  targetSlotIndex: number | null
}): Promise<void> {
  await apiClient.put('/api/admin/bracket-edges', body)
}

// 어드민 보정 간선 해제 — 다음 동기화가 자동 연결로 복구
export async function deleteBracketEdge(edgeId: number): Promise<void> {
  await apiClient.delete(`/api/admin/bracket-edges/${edgeId}`)
}
