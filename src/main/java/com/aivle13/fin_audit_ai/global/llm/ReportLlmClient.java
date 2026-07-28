package com.aivle13.fin_audit_ai.global.llm;

public interface ReportLlmClient {

    String generate(String systemPrompt, String userPrompt);
}