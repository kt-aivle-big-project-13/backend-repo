package com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;

import java.util.Comparator;
import java.util.List;

public record SelfCheckAnswerResponse(
        Long auditId,
        List<Item> answers
) {

    public static SelfCheckAnswerResponse of(Long auditId, List<SelfCheckAnswerEntity> entities) {
        List<Item> items = entities.stream()
                .sorted(Comparator.comparing(SelfCheckAnswerEntity::getItemCode))
                .map(Item::from)
                .toList();

        return new SelfCheckAnswerResponse(auditId, items);
    }

    public record Item(
            SelfCheckItemCode itemCode,
            String label,
            SelfCheckAnswerValue answer
    ) {
        public static Item from(SelfCheckAnswerEntity entity) {
            return new Item(
                    entity.getItemCode(),
                    entity.getItemCode().label(),
                    entity.getAnswer()
            );
        }
    }
}
