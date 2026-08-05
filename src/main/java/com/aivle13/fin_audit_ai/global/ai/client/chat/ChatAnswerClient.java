package com.aivle13.fin_audit_ai.global.ai.client.chat;

import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.response.ChatAnswerResponse;

public interface ChatAnswerClient {

    ChatAnswerResponse generate(
            ChatAnswerRequest request
    );
}
