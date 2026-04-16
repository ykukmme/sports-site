import { useTeams } from '../hooks/useTeams'
import { TeamCard } from '../components/team/TeamCard'
import { LoadingSpinner } from '../components/common/LoadingSpinner'
import { ErrorMessage } from '../components/common/ErrorMessage'
import { EmptyState } from '../components/common/EmptyState'

// 팀 목록 페이지
export function TeamsPage() {
  const { data: teams, isLoading, error } = useTeams()

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">팀</h1>

      {isLoading && <LoadingSpinner />}
      {error && <ErrorMessage message={error.message} />}
      {!isLoading && !error && teams?.length === 0 && (
        <EmptyState message="팀 정보가 없습니다." />
      )}
      {teams && teams.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {teams.map((team) => (
            <TeamCard key={team.id} team={team} />
          ))}
        </div>
      )}
    </div>
  )
}
