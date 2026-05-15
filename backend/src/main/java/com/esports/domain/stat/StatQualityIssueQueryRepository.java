package com.esports.domain.stat;

import com.esports.domain.matchexternal.ExternalDetailStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StatQualityIssueQueryRepository {

    private final EntityManager entityManager;

    public StatQualityIssueQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<StatQualityMatchIssueProjection> findIssues(StatQualityIssueType type, Pageable pageable) {
        String whereClause = buildWhereClause(type);
        String selectJpql = """
                select new com.esports.domain.stat.StatQualityMatchIssueProjection(
                    m.id,
                    coalesce(m.internationalCompetitionCode, ta.league),
                    m.tournamentName,
                    ta.name,
                    tb.name,
                    m.scheduledAt,
                    d.status,
                    d.expectedGameCount,
                    d.syncedGameCount,
                    (select count(tgs) from TeamGameStat tgs where tgs.match.id = m.id),
                    (select count(pgs) from PlayerGameStat pgs where pgs.match.id = m.id),
                    (select case when count(g) > 0 then true else false end
                     from MatchExternalDetailGame g
                     where g.detail.match.id = m.id
                       and (g.blueTeamId is null or g.redTeamId is null)),
                    (select case when count(pu) > 0 then true else false end
                     from PlayerGameStat pu
                     where pu.match.id = m.id
                       and pu.player is null),
                    (select case when count(pl) > 0 then true else false end
                     from PlayerGameStat pl
                     where pl.match.id = m.id
                       and (pl.gd15 is null or pl.xpd15 is null or pl.csd15 is null)),
                    (select case when count(pv) > 0 then true else false end
                     from PlayerGameStat pv
                     where pv.match.id = m.id
                       and pv.visionScore is null)
                )
                from MatchExternalDetail d
                join d.match m
                join m.teamA ta
                join m.teamB tb
                """ + whereClause + """
                order by m.scheduledAt desc
                """;

        String countJpql = """
                select count(d)
                from MatchExternalDetail d
                join d.match m
                """ + whereClause;

        TypedQuery<StatQualityMatchIssueProjection> query = entityManager.createQuery(
                selectJpql,
                StatQualityMatchIssueProjection.class
        );
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        applyParameters(query, type);
        applyParameters(countQuery, type);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<StatQualityMatchIssueProjection> rows = query.getResultList();
        long total = countQuery.getSingleResult();
        return new PageImpl<>(rows, pageable, total);
    }

    public long countMissingSideTeamGames() {
        return entityManager.createQuery("""
                        select count(g)
                        from MatchExternalDetailGame g
                        where g.blueTeamId is null or g.redTeamId is null
                        """, Long.class)
                .getSingleResult();
    }

    private String buildWhereClause(StatQualityIssueType type) {
        List<String> clauses = new ArrayList<>();
        if (type == null) {
            clauses.add(anyIssueClause());
        } else {
            clauses.add(issueClause(type));
        }
        return "where " + String.join(" and ", clauses) + "\n";
    }

    private String anyIssueClause() {
        return "(" + String.join(" or ", List.of(
                issueClause(StatQualityIssueType.NO_TEAM_STATS),
                issueClause(StatQualityIssueType.NO_PLAYER_STATS),
                issueClause(StatQualityIssueType.MISSING_SIDE_TEAM),
                issueClause(StatQualityIssueType.PARTIAL_DETAIL),
                issueClause(StatQualityIssueType.FAILED_DETAIL),
                issueClause(StatQualityIssueType.NEEDS_REVIEW),
                issueClause(StatQualityIssueType.MISSING_LANING),
                issueClause(StatQualityIssueType.MISSING_VISION),
                issueClause(StatQualityIssueType.UNMATCHED_PLAYER)
        )) + ")";
    }

    private String issueClause(StatQualityIssueType type) {
        return switch (type) {
            case NO_TEAM_STATS -> """
                    d.status = :syncedStatus
                    and not exists (
                        select 1 from TeamGameStat tgs where tgs.match.id = m.id
                    )
                    """;
            case NO_PLAYER_STATS -> """
                    d.status = :syncedStatus
                    and not exists (
                        select 1 from PlayerGameStat pgs where pgs.match.id = m.id
                    )
                    """;
            case MISSING_SIDE_TEAM -> """
                    exists (
                        select 1 from MatchExternalDetailGame g
                        where g.detail.match.id = m.id
                          and (g.blueTeamId is null or g.redTeamId is null)
                    )
                    """;
            case PARTIAL_DETAIL -> "d.status = :partialStatus";
            case FAILED_DETAIL -> "d.status = :failedStatus";
            case NEEDS_REVIEW -> "d.status = :needsReviewStatus";
            case MISSING_LANING -> """
                    exists (
                        select 1 from PlayerGameStat pl
                        where pl.match.id = m.id
                          and (pl.gd15 is null or pl.xpd15 is null or pl.csd15 is null)
                    )
                    """;
            case MISSING_VISION -> """
                    exists (
                        select 1 from PlayerGameStat pv
                        where pv.match.id = m.id
                          and pv.visionScore is null
                    )
                    """;
            case UNMATCHED_PLAYER -> """
                    exists (
                        select 1 from PlayerGameStat pu
                        where pu.match.id = m.id
                          and pu.player is null
                    )
                    """;
        };
    }

    private void applyParameters(TypedQuery<?> query, StatQualityIssueType type) {
        if (type == null || type == StatQualityIssueType.NO_TEAM_STATS || type == StatQualityIssueType.NO_PLAYER_STATS) {
            query.setParameter("syncedStatus", ExternalDetailStatus.SYNCED);
        }
        if (type == null || type == StatQualityIssueType.PARTIAL_DETAIL) {
            query.setParameter("partialStatus", ExternalDetailStatus.PARTIAL_SYNC);
        }
        if (type == null || type == StatQualityIssueType.FAILED_DETAIL) {
            query.setParameter("failedStatus", ExternalDetailStatus.FAILED);
        }
        if (type == null || type == StatQualityIssueType.NEEDS_REVIEW) {
            query.setParameter("needsReviewStatus", ExternalDetailStatus.NEEDS_REVIEW);
        }
    }
}
