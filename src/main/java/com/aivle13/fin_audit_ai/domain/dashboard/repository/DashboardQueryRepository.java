package com.aivle13.fin_audit_ai.domain.dashboard.repository;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.AuditStatusCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.DashboardSummaryProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.FairnessMetricCountProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.RecentAuditProjection;
import com.aivle13.fin_audit_ai.domain.dashboard.repository.projection.ReviewRequiredModelProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

// "최신 감사만 집계"하는 서브쿼리는 전부 model.modelGroupId(모델 계열) 기준으로 최신을 가린다.
// 모델을 재업로드할 때마다 새 버전이 새 ai_models row(새 model.id)로 생기므로, model.id로
// 비교하면 같은 계열의 옛날 버전 감사까지 전부 "최신"으로 잡혀 중복 집계된다(직접 재현해서
// 확인함: test_v3 계열 1.0.0~4.0.0 감사 4건이 대시보드 TOP5·도넛차트에 전부 따로 잡힘).
public interface DashboardQueryRepository extends Repository<AuditEntity, Long> {

    @Query("""
            select
                count(a.id) as analyzedModelCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT
                    then 1 else 0 end), 0) as normalModelCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING
                    then 1 else 0 end), 0) as reviewRequiredCount,
                coalesce(sum(case
                    when a.status = com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
                    then 1 else 0 end), 0) as thresholdExceededCount
            from AuditEntity a
            where a.id = (
                select max(latest.id)
                from AuditEntity latest
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
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
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
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
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
                  )
            )
              and result.status in (
                com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus.REVIEW,
                com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus.FAIL
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
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
                  )
            )
              and result.status in (
                com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus.WARNING,
                com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus.REVIEW
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
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
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
                where latest.model.modelGroupId = a.model.modelGroupId
                  and latest.status in (
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                    com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
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
                com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.COMPLIANT,
                com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.WARNING,
                com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus.NON_COMPLIANT
              )
            order by a.completedAt desc, a.id desc
            """)
    List<RecentAuditProjection> findRecentAudits();
}
