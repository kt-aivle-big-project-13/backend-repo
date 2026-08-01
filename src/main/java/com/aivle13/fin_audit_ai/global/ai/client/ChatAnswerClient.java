package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerResponse;

public interface ChatAnswerClient {

    ChatAnswerResponse generate(
            ChatAnswerRequest request
    );
}
