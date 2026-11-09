package com.cnsHackathon.cns.llm;

import com.cnsHackathon.cns.global.support.Texts;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 gemini.* 설정. 키는 application-local.yaml이나 GEMINI_API_KEY 환경변수로 넣는다.
 *
 * @param model           기본 모델. turn/report에서 model을 비우면 이 모델을 쓴다.
 * @param fallbackModel   첫 시도가 실패하면(혼잡 503, 시간 초과 등) 두 번째 시도에 쓰는 모델. 비우면 같은 모델로 다시 시도한다.
 * @param safetyThreshold 괴롭힘·위험 콘텐츠 필터 기준. 사기범 역할극이 막히지 않게 BLOCK_ONLY_HIGH를 쓴다.
 * @param turn            통화 중 사기범 발화 생성 설정 (빠른 응답이 중요)
 * @param report          통화 후 평가 설정 (정확도가 중요)
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model,
        String fallbackModel,
        String safetyThreshold,
        Generation turn,
        Generation report) {

    public GeminiProperties {
        baseUrl = Texts.hasText(baseUrl) ? baseUrl : "https://generativelanguage.googleapis.com/v1beta";
        model = Texts.hasText(model) ? model : "gemini-3.6-flash";
        safetyThreshold = Texts.hasText(safetyThreshold) ? safetyThreshold : "BLOCK_ONLY_HIGH";
        turn = turn == null ? new Generation(null, "LOW", 2048, null) : turn;
        report = report == null ? new Generation(null, null, 8192, null) : report;
    }

    /**
     * @param model           비우면 gemini.model을 쓴다.
     * @param thinkingLevel   MINIMAL, LOW, MEDIUM, HIGH 중 하나. 비우면 모델 기본값.
     *                        생각 기능이 없는 모델에 넣으면 400 오류가 나므로 그때는 비운다.
     * @param maxOutputTokens 생각에 쓰는 토큰도 포함된다. 너무 작으면 답이 잘린다.
     * @param timeout         한 번 호출할 때 기다리는 최대 시간
     */
    public record Generation(String model, String thinkingLevel, int maxOutputTokens, Duration timeout) {

        public Generation {
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        }
    }

    public boolean hasApiKey() {
        return Texts.hasText(apiKey);
    }

    Generation settingsFor(LlmTask task) {
        return task == LlmTask.TURN ? turn : report;
    }

    /** 시도 순서대로의 모델. 예: [gemini-3.6-flash, gemini-3.5-flash-lite] */
    List<String> modelsFor(LlmTask task) {
        String override = settingsFor(task).model();
        String primary = Texts.hasText(override) ? override : model;
        List<String> models = new ArrayList<>(List.of(primary));
        if (Texts.hasText(fallbackModel) && !fallbackModel.equals(primary)) {
            models.add(fallbackModel);
        }
        return models;
    }
}
