import { CalendarDays, Trophy, Users, Heart } from 'lucide-react'

// 메뉴 목록 — 헤더(데스크톱)와 하단 탭바(모바일) 공용
export const NAV_ITEMS = [
  { to: '/matches/upcoming', label: '경기 일정', shortLabel: '일정', icon: CalendarDays },
  { to: '/matches/results', label: '경기 결과', shortLabel: '결과', icon: Trophy },
  { to: '/teams', label: '팀', shortLabel: '팀', icon: Users },
  { to: '/cheer', label: '응원하기', shortLabel: '응원', icon: Heart },
] as const
