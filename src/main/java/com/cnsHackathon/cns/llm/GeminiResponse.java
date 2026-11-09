package com.cnsHackathon.cns.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** generateContent 응답에서 쓰는 부분만 읽는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {

    public Optional<Candidate> firstCandidate() {
        return candidates == null || candidates.isEmpty() ? Optional.empty() : Optional.ofNullable(candidates.get(0));
    }

    public String blockReason() {
        return promptFeedback == null ? null : promptFeedback.blockReason();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) {

        /** 생각(thought) 부분을 빼고 답변 글자만 이어 붙인다. */
        public String text() {
            if (content == null || content.parts() == null) {
                return "";
            }
            return content.parts().stream()
                    .filter(part -> !Boolean.TRUE.equals(part.thought()))
                    .map(Part::text)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text, Boolean thought) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(String blockReason) {
    }
}
