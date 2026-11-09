package com.cnsHackathon.cns.llm;

import java.util.List;
import java.util.Map;

/**
 * AI에 보낼 요청 한 건.
 *
 * @param systemInstruction 역할과 규칙
 * @param messages          대화 내용. user로 시작해서 user/model이 번갈아 나온다.
 * @param responseSchema    응답 JSON 형식 (JsonSchemas로 만든다)
 */
public record LlmPrompt(
        LlmTask task,
        String systemInstruction,
        List<LlmMessage> messages,
        Map<String, Object> responseSchema) {

    public LlmPrompt {
        messages = List.copyOf(messages);
    }
}
