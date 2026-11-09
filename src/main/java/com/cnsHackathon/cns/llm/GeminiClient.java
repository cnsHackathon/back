package com.cnsHackathon.cns.llm;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Gemini generateContent REST API 호출. 응답은 항상 JSON 스키마에 맞춘 객체로 받는다.
 * 일시적인 실패(429, 5xx, 시간 초과, 형식 오류, 잘린 응답)는 한 번 더 시도하고,
 * fallback-model이 있으면 두 번째 시도는 그 모델로 한다. (모델 하나가 혼잡해도 통화가 이어지게)
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final int MAX_ATTEMPTS = 2;
    private static final List<String> RELAXED_CATEGORIES =
            List.of("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_DANGEROUS_CONTENT");

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<LlmTask, RestClient> restClients = new EnumMap<>(LlmTask.class);

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        for (LlmTask task : LlmTask.values()) {
            restClients.put(task, restClient(properties.baseUrl(), properties.settingsFor(task).timeout()));
        }
        if (!properties.hasApiKey()) {
            log.warn("Gemini API 키가 없어요. application-local.yaml의 gemini.api-key나 GEMINI_API_KEY를 설정해 주세요.");
        }
    }

    /**
     * @param isUsable 스키마는 맞지만 내용이 비어 있는 경우를 걸러 낸다. false면 한 번 더 시도한다.
     */
    public <T> T generateJson(LlmPrompt prompt, Class<T> type, Predicate<T> isUsable) {
        if (!properties.hasApiKey()) {
            throw new LlmException("Gemini API 키가 설정되지 않았어요.", false);
        }
        List<String> models = properties.modelsFor(prompt.task());
        byte[] body = objectMapper.writeValueAsBytes(requestBody(prompt));

        LlmException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String model = models.get(Math.min(attempt, models.size()) - 1);
            long startedAt = System.nanoTime();
            try {
                T result = readResult(call(prompt.task(), model, body), type);
                if (!isUsable.test(result)) {
                    throw new LlmException("응답 내용이 비어 있거나 조건에 맞지 않아요.", true);
                }
                log.info("Gemini {} 성공: model={}, {}ms, 시도 {}회",
                        prompt.task(), model, elapsedMillis(startedAt), attempt);
                return result;
            } catch (LlmException e) {
                lastFailure = e;
                log.warn("Gemini {} 실패: model={}, {}ms, 시도 {}회, {}",
                        prompt.task(), model, elapsedMillis(startedAt), attempt, e.getMessage());
                if (!e.isRetryable()) {
                    break;
                }
                if (attempt < MAX_ATTEMPTS) {
                    pause(e.isRateLimited() ? Duration.ofSeconds(2) : Duration.ofMillis(300));
                }
            }
        }
        throw lastFailure;
    }

    private Map<String, Object> requestBody(LlmPrompt prompt) {
        GeminiProperties.Generation settings = properties.settingsFor(prompt.task());

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseJsonSchema", prompt.responseSchema());
        generationConfig.put("maxOutputTokens", settings.maxOutputTokens());
        if (settings.thinkingLevel() != null && !settings.thinkingLevel().isBlank()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", settings.thinkingLevel()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", prompt.systemInstruction()))));
        body.put("contents", prompt.messages().stream()
                .map(message -> Map.of(
                        "role", message.role().apiName(),
                        "parts", List.of(Map.of("text", message.text()))))
                .toList());
        body.put("generationConfig", generationConfig);
        body.put("safetySettings", RELAXED_CATEGORIES.stream()
                .map(category -> Map.of("category", category, "threshold", properties.safetyThreshold()))
                .toList());
        return body;
    }

    private GeminiResponse call(LlmTask task, String model, byte[] body) {
        try {
            byte[] raw = restClients.get(task).post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
            if (raw == null) {
                throw new LlmException("빈 응답이 왔어요.", true);
            }
            return objectMapper.readValue(raw, GeminiResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            boolean retryable = status == 429 || status >= 500;
            throw new LlmException("HTTP " + status + " " + abbreviate(e.getResponseBodyAsString()), retryable, status == 429);
        } catch (ResourceAccessException e) {
            throw new LlmException("연결하지 못했거나 시간이 초과됐어요: " + e.getMessage(), true);
        } catch (RestClientException e) {
            // 응답 본문을 읽다가 시간이 초과되는 경우 등.
            throw new LlmException(e.getClass().getSimpleName() + ": " + e.getMessage(), true);
        } catch (JacksonException e) {
            throw new LlmException("응답을 읽지 못했어요: " + e.getMessage(), true);
        }
    }

    private <T> T readResult(GeminiResponse response, Class<T> type) {
        if (response.blockReason() != null) {
            throw new LlmException("요청이 안전 필터에 막혔어요: " + response.blockReason(), false);
        }
        GeminiResponse.Candidate candidate = response.firstCandidate()
                .orElseThrow(() -> new LlmException("응답 후보가 없어요.", true));
        String text = candidate.text();
        String finishReason = candidate.finishReason();
        if (text.isBlank() || (finishReason != null && !"STOP".equals(finishReason))) {
            throw new LlmException("응답이 완성되지 않았어요: finishReason=" + finishReason, true);
        }
        try {
            return objectMapper.readValue(stripCodeFence(text), type);
        } catch (JacksonException e) {
            throw new LlmException("JSON 형식이 맞지 않아요: " + abbreviate(text), true);
        }
    }

    private static RestClient restClient(String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    static String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int closing = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closing <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, closing).trim();
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "…";
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
