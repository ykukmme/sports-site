package com.esports.domain.pandascore;

import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.Match;
import com.esports.domain.match.MatchExternalSource;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.match.MatchStatus;
import com.esports.domain.team.Team;
import com.esports.domain.team.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

// PandaScore 데이터 동기화 서비스
// Hard Rule #8: 외부 데이터 검증 후 저장, 누락/이상 데이터는 REJECT
@Service
public class PandaScoreSyncService {

    private static final Logger log = LoggerFactory.getLogger(PandaScoreSyncService.class);

    private final PandaScoreApiClient apiClient;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final SyncLogRepository syncLogRepository;
    private final PandaScoreProperties properties;

    public PandaScoreSyncService(PandaScoreApiClient apiClient,
                                  MatchRepository matchRepository,
                                  TeamRepository teamRepository,
                                  SyncLogRepository syncLogRepository,
                                  PandaScoreProperties properties) {
        this.apiClient = apiClient;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.syncLogRepository = syncLogRepository;
        this.properties = properties;
    }

    // API 키 미설정 시 동기화 건너뜀
    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    // 예정/진행 중 경기 동기화 (5분 간격 스케줄러에서 호출)
    @Transactional
    public void syncUpcomingAndRunning() {
        if (!isConfigured()) return;

        try {
            List<PandaScoreApiClient.PandaScoreMatch> upcoming = apiClient.getUpcomingMatches();
            List<PandaScoreApiClient.PandaScoreMatch> running = apiClient.getRunningMatches();
            syncMatches(upcoming);
            sleepBetweenCalls();
            syncMatches(running);
        } catch (Exception e) {
            log.error("[PandaScore] 예정/진행 경기 동기화 실패: {}", e.getMessage());
        }
    }

    // 완료 경기 동기화 (10분 간격 스케줄러에서 호출)
    @Transactional
    public void syncPastMatches() {
        if (!isConfigured()) return;

        try {
            List<PandaScoreApiClient.PandaScoreMatch> past = apiClient.getPastMatches();
            syncMatches(past);
        } catch (Exception e) {
            log.error("[PandaScore] 완료 경기 동기화 실패: {}", e.getMessage());
        }
    }

    // 개별 경기 검증 및 저장
    private void syncMatches(List<PandaScoreApiClient.PandaScoreMatch> matches) {
        for (PandaScoreApiClient.PandaScoreMatch psMatch : matches) {
            try {
                String externalId = String.valueOf(psMatch.id());

                // Hard Rule #8: 필수 필드 검증
                String rejection = validateMatch(psMatch);
                if (rejection != null) {
                    syncLogRepository.save(SyncLog.rejected("MATCH", externalId, rejection));
                    log.debug("[PandaScore] 경기 거부 (externalId={}): {}", externalId, rejection);
                    continue;
                }

                // 외부 팀 매핑 (DB에 등록된 팀만 처리)
                var opponents = psMatch.opponents();
                Team teamA = findTeamByExternalId(opponents.get(0).opponent().id());
                Team teamB = findTeamByExternalId(opponents.get(1).opponent().id());

                if (teamA == null || teamB == null) {
                    syncLogRepository.save(SyncLog.rejected("MATCH", externalId,
                            "DB에 등록되지 않은 팀: teamA=" + opponents.get(0).opponent().id()
                                    + ", teamB=" + opponents.get(1).opponent().id()));
                    continue;
                }

                // 기존 경기 업데이트 또는 신규 저장
                Match existing = matchRepository.findByExternalId(externalId).orElse(null);
                if (existing != null) {
                    existing.setExternalSource(MatchExternalSource.PANDASCORE);
                    existing.setLastSyncedAt(OffsetDateTime.now());
                    updateMatchStatus(existing, psMatch.status());
                } else {
                    // 신규 경기 저장은 관리자가 직접 등록하는 방식으로 처리
                    // PandaScore는 상태 업데이트 용도로만 사용
                    log.debug("[PandaScore] 미등록 경기 스킵 (externalId={})", externalId);
                }

                syncLogRepository.save(SyncLog.success("MATCH", externalId));
                sleepBetweenCalls();

            } catch (Exception e) {
                String externalId = psMatch.id() != null ? String.valueOf(psMatch.id()) : "unknown";
                syncLogRepository.save(SyncLog.error("MATCH", externalId, e.getMessage()));
                log.error("[PandaScore] 경기 처리 오류 (externalId={}): {}", externalId, e.getMessage());
            }
        }
    }

    // Hard Rule #8: 외부 데이터 검증 규칙
    private String validateMatch(PandaScoreApiClient.PandaScoreMatch match) {
        if (match.id() == null) return "id 누락";
        if (match.status() == null || match.status().isBlank()) return "status 누락";
        if (match.opponents() == null || match.opponents().size() < 2) return "팀 참가자 2개 미만";

        var opp0 = match.opponents().get(0);
        var opp1 = match.opponents().get(1);
        if (opp0.opponent() == null || opp0.opponent().name() == null) return "A팀 정보 누락";
        if (opp1.opponent() == null || opp1.opponent().name() == null) return "B팀 정보 누락";

        if (match.scheduledAt() != null) {
            try {
                OffsetDateTime.parse(match.scheduledAt());
            } catch (DateTimeParseException e) {
                return "scheduledAt 형식 오류: " + match.scheduledAt();
            }
        }

        return null; // 검증 통과
    }

    // PandaScore 상태 → MatchStatus 매핑
    private void updateMatchStatus(Match match, String pandaStatus) {
        MatchStatus newStatus = switch (pandaStatus.toLowerCase()) {
            case "running" -> MatchStatus.ONGOING;
            case "finished" -> MatchStatus.COMPLETED;
            case "canceled", "postponed" -> MatchStatus.CANCELLED;
            default -> null;
        };
        if (newStatus != null && match.getStatus() != newStatus) {
            match.setStatus(newStatus);
            matchRepository.save(match);
        }
    }

    // 팀 외부 ID로 DB 팀 조회
    private Team findTeamByExternalId(Long externalId) {
        if (externalId == null) return null;
        return teamRepository.findByExternalId(String.valueOf(externalId)).orElse(null);
    }

    // API 호출 간 100ms 딜레이 (Rate Limit 방지)
    private void sleepBetweenCalls() {
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
    }
}
