package com.aivle13.fin_audit_ai.global.exception.board;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AttachmentNotFoundException extends BusinessException {
    public AttachmentNotFoundException() {
        super(ErrorCode.ATTACHMENT_NOT_FOUND);
    }
}