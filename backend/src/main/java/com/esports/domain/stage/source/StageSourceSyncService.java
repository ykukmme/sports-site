package com.esports.domain.stage.source;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTeam;
import com.esports.domain.pandascore.SyncLog;
import com.esports.domain.pandascore.SyncLogRepository;
import com.esports.domain.stage.CompetitionStage;
import com.esports.domain.stage.CompetitionStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;

// 단계 원본 경기 스냅샷 동기화 (설계 §14.4.1)
// 외부 조회는 트랜잭션 밖, 검증 통과 시에만 단계 잠금 안에서 전체 교체. 실패·REJECT 면 기존 스냅샷 유지
@Service
public class StageSourceSyncService {

    private static final Logger log = LoggerFactory.getLogger(StageSourceSyncService.class);
    private static final String LOG_RESOURCE = "STAGE_SOURCE";
    // 100건 × 3페이지 — 관측된 최대 단계는 90경기 (설계 §2.1)
    static final int MAX_PAGES = 3;

    private final CompetitionStageService stageService;
    private final StageSourceMatchRepository sourceMatchRepository;
    private final SyncLogRepository syncLogRepository;
    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;
    private final TransactionTemplate transactionTemplate;

    public StageSourceSyncService(CompetitionStageService stageService,
                                  StageSourceMatchRepository sourceMatchRepository,
                                  SyncLogRepository syncLogRepository,
                                  PandaScoreApiClient apiClient,
                                  PandaScoreProperties properties,
                                  PlatformTransactionManager transactionManager) {
        this.stageService = stageService;
        this.sourceMatchRepository = sourceMatchRepository;
        this.syncLogRepository = syncLogRepository;
        this.apiClient = apiClient;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public StageSourceSyncResponse sync(Long stageId) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException("PANDASCORE_NOT_CONFIGURED", "PandaScore API 키가 설정되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
        Long tournamentId = stageService.getStage(stageId).getExternalTournamentId();
        String logId = String.valueOf(tournamentId);

        PandaScoreApiClient.TournamentMatches fetched;
        try {
            fetched = apiClient.getTournamentMatches(tournamentId, MAX_PAGES);
        } catch (RestClientException e) {
            syncLogRepository.save(SyncLog.error(LOG_RESOURCE, logId, e.getMessage()));
            log.error("[StageSource] PandaScore fetch failed: tournamentId={}, error={}", tournamentId, e.getMessage());
            throw new BusinessException("STAGE_SOURCE_FETCH_FAILED", "PandaScore 원본 경기 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        // Hard Rule #8 — 검증 실패 시 기존 스냅샷 유지, 전체 REJECT
        List<String> reasons = StageSourceValidator.validate(tournamentId, fetched.matches(), fetched.truncated());
        if (!reasons.isEmpty()) {
            syncLogRepository.save(SyncLog.rejected(LOG_RESOURCE, logId, String.join(" | ", reasons)));
            log.warn("[StageSource] rejected: tournamentId={}, reasons={}", tournamentId, reasons);
            return StageSourceSyncResponse.rejected(reasons);
        }

        List<PandaScoreMatch> validated = fetched.matches();
        return transactionTemplate.execute(status -> replaceSnapshot(stageId, validated));
    }

    private StageSourceSyncResponse replaceSnapshot(Long stageId, List<PandaScoreMatch> matches) {
        CompetitionStage stage = stageService.lockStage(stageId);
        sourceMatchRepository.deleteByStageId(stage.getId());

        List<StageSourceMatch> rows = matches.stream().map(match -> toRow(stage, match)).toList();
        sourceMatchRepository.saveAll(rows);

        // 잠금으로 조회한 관리 상태 엔티티 — 커밋 시 dirty checking 으로 source_synced_at 반영
        // deleteByStageId 에 clearAutomatically 를 켜면 이 엔티티가 분리되어 갱신이 유실되므로 금지
        OffsetDateTime now = OffsetDateTime.now();
        stage.markSourceSynced(now);
        syncLogRepository.save(SyncLog.success(LOG_RESOURCE, String.valueOf(stage.getExternalTournamentId())));

        int tbd = (int) rows.stream().filter(row -> row.getExclusionReason() == SourceExclusionReason.TBD).count();
        int cancelled = (int) rows.stream().filter(row -> row.getExclusionReason() == SourceExclusionReason.CANCELLED).count();
        log.info("[StageSource] synced: stageId={}, total={}, tbd={}, cancelled={}", stage.getId(), rows.size(), tbd, cancelled);
        return StageSourceSyncResponse.synced(rows.size(), tbd, cancelled, now);
    }

    // 검증을 통과한 경기만 들어온다 — 승자는 종료 경기에만 기록
    private static StageSourceMatch toRow(CompetitionStage stage, PandaScoreMatch match) {
        PandaScoreTeam a = opponent(match, 0);
        PandaScoreTeam b = opponent(match, 1);
        OffsetDateTime scheduledAt = match.scheduledAt() != null ? OffsetDateTime.parse(match.scheduledAt()) : null;
        Long winner = StageSourceMatch.STATUS_FINISHED.equals(match.status()) ? match.winnerId() : null;
        return new StageSourceMatch(stage, match.id(), scheduledAt, match.status(),
                a != null ? a.id() : null, a != null ? a.name() : null,
                b != null ? b.id() : null, b != null ? b.name() : null,
                winner);
    }

    private static PandaScoreTeam opponent(PandaScoreMatch match, int index) {
        if (match.opponents() == null || match.opponents().size() <= index) {
            return null;
        }
        return match.opponents().get(index).opponent();
    }
}
