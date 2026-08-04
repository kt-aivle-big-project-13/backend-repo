package com.aivle13.fin_audit_ai.domain.dashboard.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.AuditStatusCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.DashboardSummaryProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.FairnessMetricCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.RecentAuditProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.ReviewRequiredModelProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DashboardQueryRepository extends Repository<AuditEntity, Long> {

    @Query("""
            select
                count(a.id) as analyzedModelCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT
                    then 1 else 0 end), 0) as normalModelCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING
                    then 1 else 0 end), 0) as reviewRequiredCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                    then 1 else 0 end), 0) as thresholdExceededCount
            from AuditEntity a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
            """)
    DashboardSummaryProjection findSummary();

    @Query("""
            select
                a.status as status,
                count(a.id) as count
            from AuditEntity a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
            group by a.status
            """)
    List<AuditStatusCountProjection> countLatestAuditsByStatus();

    @Query("""
            select
                a.model.id as modelId,
                a.model.modelName as modelName,
                a.model.version as version,
                count(result.id) as issueCount,
                a.status as status
            from FairnessResultEntity result
            join result.audit a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
              and result.status in (
                com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus.REVIEW,
                com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus.FAIL
              )
            group by a.model.id, a.model.modelName, a.model.version, a.status
            """)
    List<ReviewRequiredModelProjection> countFairnessIssuesByModel();

    @Query("""
            select
                a.model.id as modelId,
                a.model.modelName as modelName,
                a.model.version as version,
                count(result.id) as issueCount,
                a.status as status
            from XaiResultEntity result
            join result.audit a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
              and result.status in (
                com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus.WARNING,
                com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus.REVIEW
              )
            group by a.model.id, a.model.modelName, a.model.version, a.status
            """)
    List<ReviewRequiredModelProjection> countXaiIssuesByModel();

    @Query("""
            select
                result.metricCode as metricCode,
                result.status as status,
                count(result.id) as count
            from FairnessResultEntity result
            join result.audit a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
            group by result.metricCode, result.status
            """)
    List<FairnessMetricCountProjection> countFairnessMetricsByStatus();

    @Query("""
            select result
            from FairnessResultEntity result
            join fetch result.audit a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.id = a.model.id
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
                  )
            )
            """)
    List<FairnessResultEntity> findLatestFairnessResults();

    @Query("""
            select result
            from FairnessResultEntity result
            join fetch result.audit
            where result.audit.id in :auditIds
            """)
    List<FairnessResultEntity> findFairnessResultsByAuditIds(
            @Param("auditIds") Collection<Long> auditIds
    );

    @Query("""
            select result
            from XaiResultEntity result
            join fetch result.audit
            where result.audit.id in :auditIds
            """)
    List<XaiResultEntity> findXaiResultsByAuditIds(
            @Param("auditIds") Collection<Long> auditIds
    );

    @Query("""
            select
                a.id as auditId,
                a.model.id as modelId,
                a.model.modelName as modelName,
                a.model.version as version,
                a.status as status,
                a.completedAt as completedAt
            from AuditEntity a
            where a.status in (
                com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.COMPLIANT,
                com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.WARNING,
                com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus.NON_COMPLIANT
              )
            order by a.completedAt desc, a.id desc
            """)
    List<RecentAuditProjection> findRecentAudits(Pageable pageable);
}
