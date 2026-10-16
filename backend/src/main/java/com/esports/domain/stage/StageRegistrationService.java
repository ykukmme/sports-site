package com.esports.domain.stage;

import com.esports.common.exception.BusinessException;
import com.esports.config.PandaScoreProperties;
import com.esports.domain.match.MatchRepository;
import com.esports.domain.pandascore.PandaScoreApiClient;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreMatch;
import com.esports.domain.pandascore.PandaScoreApiClient.PandaScoreTournamentDetail;
import com.esports.domain.pandascore.SyncLog;
import com.esports.domain.pandascore.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 대회 단계 등록 공통 경로 — 경기 가져오기·결과 동기화·어드민 등록/복구가 모두 여기서 upsert 한다 (설계 §14.1)
// 외부 조회는 트랜잭션 밖에서 수행하고, 저장은 CompetitionStageService.ensureStage 의 별도 트랜잭션으로 처리한다
@Service
public class StageRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(StageRegistrationService.class);
    private static final String LOG_RESOURCE = "STAGE";
    private static final String LOL_VIDEOGAME_SLUG = "league-of-legends";

    private final CompetitionStageService stageService;
    private final CompetitionStageRepository stageRepository;
    private final MatchRepository matchRepository;
    private final SyncLogRepository syncLogRepository;
    private final PandaScoreApiClient apiClient;
    private final PandaScoreProperties properties;

    public StageRegistrationService(CompetitionStageService stageService,
                                    CompetitionStageRepository stageRepository,
                                    MatchRepository matchRepository,
                                    SyncLogRepository syncLogRepository,
                                    PandaScoreApiClient apiClient,
                                    PandaScoreProperties properties) {
        this.stageService = stageService;
        this.stageRepository = stageRepository;
        this.matchRepository = matchRepository;
        this.syncLogRepository = syncLogRepository;
        this.apiClient = apiClient;
        this.properties = properties;
    }

    // 경기 가져오기 경로 — 단계 등록 실패는 경고만 남기고 null 반환
    // 호출 측 배치(경기 저장) 트랜잭션이 단계 등록 실패로 롤백되지 않도록 예외를 여기서 막는다
    public CompetitionStage upsert(Long externalTournamentId, String leagueName, String seriesName, String stageName) {
        try {
            return upsert(externalTournamentId, leagueName, seriesName, stageName, null);
        } catch (RuntimeException e) {
            log.warn("[Stage] upsert failed, skipped: tournamentId={}, error={}", externalTournamentId, e.getMessage());
            return null;
        }
    }

    private CompetitionStage upsert(Long externalTournamentId, String leagueName, String seriesName, String stageName,
                                    Boolean hasBracket) {
        try {
            return stageService.ensureStage(externalTournamentId, leagueName, seriesName, stageName, hasBracket);
        } catch (DataIntegrityViolationException e) {
            return stageService.ensureStage(externalTournamentId, leagueName, seriesName, stageName, hasBracket);
        }
    }

    // 결과 동기화 경로 — 경기 응답에 tournament 정보가 있으면 단계 upsert. 없으면 null
    public CompetitionStage upsertFromMatch(PandaScoreMatch match) {
        if (match == null || match.tournament() == null || match.tournament().id() == null) {
            return null;
        }
        String leagueName = StageLeagueNames.resolve(
                match.league() != null ? match.league().id() : null,
                match.league() != null ? match.league().name() : null,
                match.name(),
                match.league() != null ? match.league().name() : null,
                match.league() != null ? match.league().slug() : null,
                match.tournament().name(),
                match.tournament().slug()
        );
        String seriesName = match.serie() != null ? match.serie().fullName() : null;
        return upsert(match.tournament().id(), leagueName, seriesName, match.tournament().name());
    }

    // 어드민 등록 — tournament 상세를 조회·검증한 뒤 upsert. 경기·팀은 생성하지 않는다
    public StageRegisterResponse register(Long externalTournamentId) {
        requireApiKey();
        Registered registered = registerFromPandaScore(externalTournamentId);
        log.info("[Stage] registered by admin: tournamentId={}, stageId={}, status={}",
                externalTournamentId, registered.stage().getId(), registered.status());
        return new StageRegisterResponse(registered.status(), toAdminResponse(registered.stage()));
    }

    // 어드민 복구 — 기존 경기에 있는 tournament id 만 등록. 한 건 실패가 나머지를 막지 않는다
    public StageBackfillResponse backfill(List<Long> externalTournamentIds) {
        requireApiKey();
        Set<Long> requested = new LinkedHashSet<>(externalTournamentIds);
        if (requested.size() > StageBackfillRequest.MAX_IDS) {
            throw new BusinessException("STAGE_BACKFILL_TOO_MANY", "한 번에 최대 100개까지 복구할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        Set<Long> existing = new HashSet<>(matchRepository.findExistingExternalTournamentIds(requested));

        List<StageBackfillResponse.Item> items = new ArrayList<>();
        for (Long tournamentId : requested) {
            if (!existing.contains(tournamentId)) {
                items.add(new StageBackfillResponse.Item(tournamentId, StageRegistrationStatus.SKIPPED, null,
                        "등록된 경기에 없는 tournament id 입니다."));
                continue;
            }
            try {
                Registered registered = registerFromPandaScore(tournamentId);
                items.add(new StageBackfillResponse.Item(tournamentId, registered.status(),
                        registered.stage().getId(), null));
            } catch (BusinessException e) {
                StageRegistrationStatus status = e.getHttpStatus() == HttpStatus.BAD_GATEWAY
                        ? StageRegistrationStatus.FAILED
                        : StageRegistrationStatus.REJECTED;
                items.add(new StageBackfillResponse.Item(tournamentId, status, null, e.getMessage()));
            }
        }
        StageBackfillResponse response = StageBackfillResponse.of(items);
        log.info("[Stage] backfill by admin: requested={}, created={}, updated={}, skipped={}, failed={}",
                requested.size(), response.createdCount(), response.updatedCount(),
                response.skippedCount(), response.failedCount());
        return response;
    }

    private Registered registerFromPandaScore(Long tournamentId) {
        String logId = String.valueOf(tournamentId);
        PandaScoreTournamentDetail detail;
        try {
            detail = apiClient.getTournament(tournamentId);
        } catch (HttpClientErrorException.NotFound e) {
            syncLogRepository.save(SyncLog.rejected(LOG_RESOURCE, logId, "tournament not found"));
            throw new BusinessException("STAGE_TOURNAMENT_NOT_FOUND",
                    "PandaScore 에 없는 tournament id 입니다: " + tournamentId, HttpStatus.BAD_REQUEST);
        } catch (RestClientException e) {
            syncLogRepository.save(SyncLog.error(LOG_RESOURCE, logId, e.getMessage()));
            log.error("[Stage] PandaScore tournament fetch failed: tournamentId={}, error={}", tournamentId, e.getMessage());
            throw new BusinessException("STAGE_FETCH_FAILED", "PandaScore 대회 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        // Hard Rule #8 — 누락·불일치 응답은 저장하지 않는다
        List<String> reasons = validate(tournamentId, detail);
        if (!reasons.isEmpty()) {
            syncLogRepository.save(SyncLog.rejected(LOG_RESOURCE, logId, String.join(" | ", reasons)));
            log.warn("[Stage] tournament rejected: tournamentId={}, reasons={}", tournamentId, reasons);
            throw new BusinessException("STAGE_TOURNAMENT_REJECTED", String.join(" ", reasons), HttpStatus.BAD_REQUEST);
        }

        boolean existed = stageRepository.findByExternalTournamentId(tournamentId).isPresent();
        String leagueName = StageLeagueNames.resolve(
                detail.league().id(),
                detail.league().name(),
                detail.league().name(),
                detail.league().slug(),
                detail.name(),
                detail.slug()
        );
        String seriesName = detail.serie() != null ? detail.serie().fullName() : null;
        CompetitionStage stage = upsert(tournamentId, leagueName, seriesName, detail.name(),
                Boolean.TRUE.equals(detail.hasBracket()));
        syncLogRepository.save(SyncLog.success(LOG_RESOURCE, logId));
        return new Registered(stage, existed ? StageRegistrationStatus.UPDATED : StageRegistrationStatus.CREATED);
    }

    // tournament 상세 검증 — 요청 id 일치, 대회명, 리그명, LoL 여부
    static List<String> validate(Long requestedId, PandaScoreTournamentDetail detail) {
        List<String> reasons = new ArrayList<>();
        if (detail == null) {
            reasons.add("PandaScore 응답이 비어 있습니다.");
            return reasons;
        }
        if (!requestedId.equals(detail.id())) {
            reasons.add("응답 tournament id 가 요청과 다릅니다.");
        }
        if (detail.name() == null || detail.name().isBlank()) {
            reasons.add("대회 단계명이 없습니다.");
        }
        if (detail.league() == null || detail.league().name() == null || detail.league().name().isBlank()) {
            reasons.add("리그 정보가 없습니다.");
        }
        if (detail.videogame() == null || detail.videogame().slug() == null) {
            reasons.add("게임 정보가 없습니다.");
        } else if (!LOL_VIDEOGAME_SLUG.equals(detail.videogame().slug())) {
            reasons.add("리그 오브 레전드 대회가 아닙니다.");
        }
        return reasons;
    }

    private AdminStageResponse toAdminResponse(CompetitionStage stage) {
        return AdminStageResponse.from(stage, matchRepository.countByExternalTournamentId(stage.getExternalTournamentId()));
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException("PANDASCORE_NOT_CONFIGURED", "PandaScore API 키가 설정되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private record Registered(CompetitionStage stage, StageRegistrationStatus status) {}
}
