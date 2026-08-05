package com.aivle13.fin_audit_ai.domain.audit.service.fairness;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessGroupStatRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.fairness.FairnessResultNotFoundException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class FairnessResultService {

    // 격차(difference) 지표 기본 임계값 — 금융 AI 가이드라인 80% Rule을 격차 기준으로
    // 근사한 값이다. 80% Rule은 본래 비율 기준(min/max ≥ 0.80)이라 Proportional Parity에만
    // 정확히 대응하고, 격차 지표에는 고정 숫자로 딱 떨어지지 않아 "집단 간 최대 20%p 이내"로
    // 근사한다(1 − 0.80 = 0.20). REVIEW 밴드는 이 값의 2배(0.40)까지다.
    private static final BigDecimal DEMOGRAPHIC_PARITY_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal EQUAL_OPPORTUNITY_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal EQUALIZED_ODDS_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal FPR_PARITY_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal FDR_PARITY_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal FOR_PARITY_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal REVIEW_THRESHOLD_MULTIPLIER = BigDecimal.valueOf(2);

    // Proportional Parity 는 80% Rule(min/max 승인율 ≥ 0.80)을 그대로 표현하는 "비율" 지표라
    // 격차 지표와 판정 방향이 반대다(격차는 "작을수록" 공정, 이건 "클수록" 공정). 그래서
    // 별도 임계값·판정 로직(judgeRatioStatus)을 쓴다. 0.80 은 금융 AI 가이드라인 기준값이다.
    private static final BigDecimal PROPORTIONAL_PARITY_MIN_RATIO = new BigDecimal("0.80");
    private static final BigDecimal PROPORTIONAL_PARITY_REVIEW_MARGIN = new BigDecimal("0.10");

    private final AuditRepository auditRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final FairnessGroupStatRepository fairnessGroupStatRepository;
    private final CacheManager cacheManager;

    // getFairness(2-arg)에서 @Cacheable이 붙은 3-arg 메서드를 this로 직접 호출하면
    // 프록시를 우회해 캐싱이 적용되지 않는다. 자기 자신의 프록시를 주입받아 그걸 통해 호출한다.
    private final FairnessResultService self;

    public FairnessResultService(
            AuditRepository auditRepository,
            FairnessResultRepository fairnessResultRepository,
            FairnessGroupStatRepository fairnessGroupStatRepository,
            CacheManager cacheManager,
            @Lazy FairnessResultService self
    ) {
        this.auditRepository = auditRepository;
        this.fairnessResultRepository = fairnessResultRepository;
        this.fairnessGroupStatRepository = fairnessGroupStatRepository;
        this.cacheManager = cacheManager;
        this.self = self;
    }

    public FairnessResultResponse getFairness(Long userId, Long auditId) {
        return self.getFairness(userId, auditId, null);
    }

    @Cacheable(cacheNames = CacheConfig.FAIRNESS_CACHE, key = "#userId + ':' + #auditId + ':' + #attribute")
    public FairnessResultResponse getFairness(Long userId, Long auditId, String attribute) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);

        if (audit.getStatus() == AuditStatus.PENDING || audit.getStatus() == AuditStatus.IN_PROGRESS) {
            throw new AuditNotCompletedException();
        }

        boolean byAttribute = attribute != null && !attribute.isBlank();

        List<FairnessResultEntity> results = byAttribute
                ? fairnessResultRepository.findAllByAudit_IdAndAttribute(auditId, attribute)
                : fairnessResultRepository.findAllByAudit_Id(auditId);

        if (results.isEmpty()) {
            throw new FairnessResultNotFoundException();
        }

        List<FairnessGroupStatEntity> groupStats = byAttribute
                ? fairnessGroupStatRepository.findAllByAudit_IdAndAttribute(auditId, attribute)
                : fairnessGroupStatRepository.findAllByAudit_Id(auditId);

        return FairnessResultResponse.of(auditId, results, groupStats, audit);
    }

    // 취소 직후 재시도가 있었으면 이 결과는 이미 지나가버린 실행 세대의 늦은 응답일
    // 수 있다. 그런 경우 저장 자체를 건너뛰어 지금 실행 중인 세대의 결과와 섞이지
    // 않게 한다. 쓰기 잠금으로 조회해 취소/재시도와의 경합도 막는다.
    @Transactional
    public void saveFairnessResult(Long auditId, int generation, FairnessRunResponse response) {
        AuditEntity audit = auditRepository.findByIdForUpdate(auditId)
                .orElseThrow(AuditNotFoundException::new);

        if (audit.isCancelled() || audit.getGeneration() != generation) {
            return;
        }

        validateResponse(response);

        List<FairnessResultEntity> results = new ArrayList<>();
        List<FairnessGroupStatEntity> groupStats = new ArrayList<>();

        for (Map.Entry<String, FairnessRunResponse.AttributeFairness> entry
                : response.fairnessByAttribute().entrySet()) {

            String attribute = entry.getKey();
            FairnessRunResponse.AttributeFairness fairness = entry.getValue();

            if (fairness == null) {
                throw new AuditFailedException();
            }

            addGroupStats(groupStats, audit, attribute, fairness.groups());

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.DEMOGRAPHIC_PARITY,
                    fairness.demographicParityDifference(),
                    DEMOGRAPHIC_PARITY_THRESHOLD,
                    fairness.note()
            );

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.EQUAL_OPPORTUNITY,
                    fairness.equalOpportunityDifference(),
                    EQUAL_OPPORTUNITY_THRESHOLD,
                    fairness.note()
            );

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.EQUALIZED_ODDS,
                    fairness.equalizedOddsDifference(),
                    EQUALIZED_ODDS_THRESHOLD,
                    fairness.note()
            );

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.FPR_PARITY,
                    fairness.fprParityDifference(),
                    FPR_PARITY_THRESHOLD,
                    fairness.note()
            );

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.FDR_PARITY,
                    fairness.fdrParityDifference(),
                    FDR_PARITY_THRESHOLD,
                    fairness.note()
            );

            addIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.FOR_PARITY,
                    fairness.forParityDifference(),
                    FOR_PARITY_THRESHOLD,
                    fairness.note()
            );

            addRatioIfPresent(
                    results, audit, attribute,
                    FairnessMetricCode.PROPORTIONAL_PARITY,
                    fairness.proportionalParityRatio(),
                    PROPORTIONAL_PARITY_MIN_RATIO,
                    fairness.note()
            );
        }

        // deleteAllByAudit_Id는 @Modifying이 없는 파생 delete라 영속성 컨텍스트에 삭제만
        // 큐잉되고, Hibernate는 flush 시 삭제보다 삽입을 먼저 내보낸다. flush 없이 바로
        // saveAll을 호출하면 재시도 등으로 재저장할 때(기존 행이 이미 있는 상태) INSERT가
        // 아직 지워지지 않은 기존 행과 유니크 제약에서 충돌한다 — 그래서 delete를 먼저
        // DB에 반영시킨 뒤 insert한다. (ExplainabilityService의 xaiResultRepository·
        // shapFeatureImportanceRepository 저장과 동일한 패턴)
        fairnessResultRepository.deleteAllByAudit_Id(auditId);
        fairnessResultRepository.flush();
        fairnessResultRepository.saveAll(results);

        fairnessGroupStatRepository.deleteAllByAudit_Id(auditId);
        fairnessGroupStatRepository.flush();
        fairnessGroupStatRepository.saveAll(groupStats);

        applyPerformance(audit, response.performance());

        evictCache(audit.getUser().getId(), auditId, response.fairnessByAttribute().keySet());
    }

    // AI 응답의 집단별 기초통계·혼동행렬을 저장 엔티티로 옮긴다. groups 가 없으면(구버전
    // 응답·표본 부족) 아무것도 추가하지 않는다.
    private void addGroupStats(
            List<FairnessGroupStatEntity> target,
            AuditEntity audit,
            String attribute,
            List<FairnessRunResponse.GroupStat> groups
    ) {
        if (groups == null) {
            return;
        }
        for (FairnessRunResponse.GroupStat group : groups) {
            target.add(FairnessGroupStatEntity.of(
                    audit, attribute, group.group(), group.n(),
                    group.approvalRate(), group.actualDefaultRate(),
                    group.tp(), group.fp(), group.tn(), group.fn(), group.auc()
            ));
        }
    }

    // 감사셋 전체 모델 성능을 audit 에 기록한다. 성능 정보가 없으면(구버전 응답) 건너뛴다.
    private void applyPerformance(AuditEntity audit, FairnessRunResponse.Performance performance) {
        if (performance == null) {
            return;
        }
        audit.applyPerformance(performance.auc(), performance.accuracy());
    }

    private void evictCache(Long userId, Long auditId, Set<String> attributes) {
        Cache cache = cacheManager.getCache(CacheConfig.FAIRNESS_CACHE);

        if (cache == null) {
            return;
        }

        cache.evict(cacheKey(userId, auditId, null));

        for (String attribute : attributes) {
            cache.evict(cacheKey(userId, auditId, attribute));
        }
    }

    private String cacheKey(Long userId, Long auditId, String attribute) {
        return userId + ":" + auditId + ":" + attribute;
    }

    private void validateResponse(FairnessRunResponse response) {
        if (response == null
                || response.fairnessByAttribute() == null
                || response.fairnessByAttribute().isEmpty()) {
            throw new AuditFailedException();
        }
    }

    // AI 응답의 개별 지표값은 null 일 수 있다(해당 집단에 정상/연체 고객이 아예 없어
    // 계산 자체가 정의되지 않는 경우 — fairness.py 의 note 참고). 그렇다고 감사 전체를
    // 실패시키면 데이터가 조금만 치우쳐도 결과를 아예 못 보게 되므로, 계산 불가능한
    // 지표만 조용히 건너뛰고 나머지 지표는 정상 저장한다.
    private void addIfPresent(
            List<FairnessResultEntity> results,
            AuditEntity audit,
            String attribute,
            FairnessMetricCode metricCode,
            BigDecimal value,
            BigDecimal threshold,
            String note
    ) {
        if (value == null) {
            return;
        }

        results.add(FairnessResultEntity.of(
                audit, attribute, metricCode, value, threshold,
                judgeStatus(value.abs(), threshold),
                note
        ));
    }

    private FairnessStatus judgeStatus(BigDecimal absValue, BigDecimal threshold) {
        if (absValue.compareTo(threshold) <= 0) {
            return FairnessStatus.PASS;
        }

        if (absValue.compareTo(threshold.multiply(REVIEW_THRESHOLD_MULTIPLIER)) <= 0) {
            return FairnessStatus.REVIEW;
        }

        return FairnessStatus.FAIL;
    }

    // Proportional Parity(80% Rule) 전용. 다른 지표(addIfPresent/judgeStatus)와 달리
    // 값이 "클수록" 공정하므로 abs() 없이 그대로 최소 기준(minRatio)과 비교한다.
    // null 처리 방침은 addIfPresent 와 동일 — 계산 불가면 이 지표만 건너뛴다.
    private void addRatioIfPresent(
            List<FairnessResultEntity> results,
            AuditEntity audit,
            String attribute,
            FairnessMetricCode metricCode,
            BigDecimal ratio,
            BigDecimal minRatio,
            String note
    ) {
        if (ratio == null) {
            return;
        }

        results.add(FairnessResultEntity.of(
                audit, attribute, metricCode, ratio, minRatio,
                judgeRatioStatus(ratio, minRatio),
                note
        ));
    }

    private FairnessStatus judgeRatioStatus(BigDecimal ratio, BigDecimal minRatio) {
        if (ratio.compareTo(minRatio) >= 0) {
            return FairnessStatus.PASS;
        }

        if (ratio.compareTo(minRatio.subtract(PROPORTIONAL_PARITY_REVIEW_MARGIN)) >= 0) {
            return FairnessStatus.REVIEW;
        }

        return FairnessStatus.FAIL;
    }
}