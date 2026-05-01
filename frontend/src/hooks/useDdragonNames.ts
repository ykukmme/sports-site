import { useQuery } from '@tanstack/react-query'

const DDRAGON_VERSION = import.meta.env.VITE_DDRAGON_VERSION ?? '16.8.1'

// Data Dragon ko_KR 사전 응답 타입 (필요 필드만)
interface ChampionPayload {
  data: Record<string, { id: string; name: string }>
}

interface ItemPayload {
  data: Record<string, { name: string }>
}

interface SummonerPayload {
  data: Record<string, { id: string; name: string }>
}

interface DdragonNames {
  champions: Map<string, string>
  items: Map<string, string>
  spells: Map<string, string>
}

const EMPTY_NAMES: DdragonNames = {
  champions: new Map(),
  items: new Map(),
  spells: new Map(),
}

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) {
    throw new Error(`ddragon fetch failed: ${url} ${res.status}`)
  }
  return (await res.json()) as T
}

async function loadDdragonNames(): Promise<DdragonNames> {
  const base = `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/data/ko_KR`
  const [champion, item, summoner] = await Promise.all([
    fetchJson<ChampionPayload>(`${base}/champion.json`),
    fetchJson<ItemPayload>(`${base}/item.json`),
    fetchJson<SummonerPayload>(`${base}/summoner.json`),
  ])

  // 챔피언: data 키가 영문 ID (예: "Aatrox")
  const champions = new Map<string, string>()
  for (const [key, value] of Object.entries(champion.data ?? {})) {
    if (value?.name) champions.set(key, value.name)
  }

  // 아이템: data 키가 숫자 ID (예: "3158")
  const items = new Map<string, string>()
  for (const [key, value] of Object.entries(item.data ?? {})) {
    if (value?.name) items.set(key, value.name)
  }

  // 스펠: data 키가 영문 ID (예: "SummonerFlash")
  const spells = new Map<string, string>()
  for (const [key, value] of Object.entries(summoner.data ?? {})) {
    if (value?.name) spells.set(key, value.name)
  }

  return { champions, items, spells }
}

export function useDdragonNames(): DdragonNames {
  const { data } = useQuery({
    queryKey: ['ddragon', 'names', DDRAGON_VERSION, 'ko_KR'],
    queryFn: loadDdragonNames,
    staleTime: 24 * 60 * 60 * 1000,
    gcTime: 7 * 24 * 60 * 60 * 1000,
    retry: 1,
  })
  // 사전 로드 실패 또는 로딩 중이면 빈 Map → 호출자가 ID로 폴백
  return data ?? EMPTY_NAMES
}
