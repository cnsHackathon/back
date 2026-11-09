package com.cnsHackathon.cns.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptTemplateTest {

    @Test
    void 자리표시자를_채운다() {
        PromptTemplate template = new PromptTemplate("test", "안녕 {{name}}, 금액은 {{amount}}원");

        assertEquals("안녕 철수, 금액은 $500원", template.render(Map.of("name", "철수", "amount", "$500")));
    }

    @Test
    void 값이_빠지면_알려_준다() {
        PromptTemplate template = new PromptTemplate("test", "{{missing}}");

        assertThrows(IllegalArgumentException.class, () -> template.render(Map.of()));
    }

    @Test
    void 코드블록으로_감싼_JSON을_벗긴다() {
        assertEquals("{\"a\":1}", GeminiClient.stripCodeFence("```json\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", GeminiClient.stripCodeFence(" {\"a\":1} "));
    }
}
