package com.aivle13.fin_audit_ai.domain.audit.validator;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditUploadRequest;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidThresholdPolicyException;
import org.springframework.stereotype.Component;

@Component
public class ThresholdPolicyValidator {

    public void validate(AuditUploadRequest request) {
        if (request.targetApprovalRate() != null) {
            double rate = request.targetApprovalRate();
            if (Double.isNaN(rate) || rate < 0 || rate > 1) {
                throw new InvalidThresholdPolicyException("목표 승인율은 0 이상 1 이하여야 합니다.");
            }
        }

        if (request.threshold() != null) {
            double threshold = request.threshold();
            if (Double.isNaN(threshold) || threshold <= 0 || threshold > 1) {
                throw new InvalidThresholdPolicyException("임계값은 0 초과 1 이하여야 합니다.");
            }
        }
    }
}
