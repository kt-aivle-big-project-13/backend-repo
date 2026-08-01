package com.aivle13.fin_audit_ai.global.exception.chat;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ChatConversationNotFoundException extends BusinessException {

    public ChatConversationNotFoundException() {
        super(ErrorCode.CHAT_CONVERSATION_NOT_FOUND);
    }
}
