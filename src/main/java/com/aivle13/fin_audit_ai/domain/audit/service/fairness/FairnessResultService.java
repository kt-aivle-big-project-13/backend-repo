package com.aivle13.fin_audit_ai.domain.audit.service.fairness;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.FairnessResultNotFoundException;
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
    private final CacheManager cacheManager;

    // getFairness(2-arg)에서 @Cacheable이 붙은 3-arg 메서드를 this로 직접 호출하면
    // 프록시를 우회해 캐싱이 적용되지 않는다. 자기 자신의 프록시를 주입받아 그걸 통해 호출한다.
    private final FairnessResultService self;

    public FairnessResultService(
            AuditRepository auditRepository,
            FairnessResultRepository fairnessResultRepository,
            CacheManager cacheManager,
            @Lazy FairnessResultService self
    ) {
        this.auditRepository = auditRepository;
        this.fairnessResultRepository = fairnessResultRepository;
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

        List<FairnessResultEntity> results =
                (attribute != null && !attribute.isBlank())
                        ? fairnessResultRepository.findAllByAudit_IdAndAttribute(auditId, attribute)
                        : fairnessResultRepository.findAllByAudit_Id(auditId);

        if (results.isEmpty()) {
            throw new FairnessResultNotFoundException();
        }

        return FairnessResultResponse.of(auditId, results);
    }

    @Transactional
    public void saveFairnessResult(Long auditId, FairnessRunResponse response) {
        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(AuditNotFoundException::new);

        validateResponse(response);

        List<FairnessResultEntity> results = new ArrayList<>();

        for (Map.Entry<String, FairnessRunResponse.AttributeFairness> entry
                : response.fairnessByAttribute().entrySet()) {

            String attribute = entry.getKey();
            FairnessRunResponse.AttributeFairness fairness = entry.getValue();

            if (fairness == null) {
                throw new AuditFailedException();
            }

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

        fairnessResultRepository.deleteAllByAudit_Id(auditId);
        fairnessResultRepository.saveAll(results);

        evictCache(audit.getUser().getId(), auditId, response.fairnessByAttribute().keySet());
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

    // TODO: 정책값 미확정. AI팀/기획 확정 후 조정 필요
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

    // TODO: 정책값 미확정. AI팀/기획 확정 후 조정 필요
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