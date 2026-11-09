package com.cnsHackathon.cns.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param isFinal 시간이 다 된 순간 녹음 중이던 답변이면 true. 이 답변을 받고 통화를 마무리한다.
 */
public record TurnRequest(
        @NotBlank @Size(max = 2000) String userText,
        @JsonProperty("isFinal") Boolean isFinal) {

    public boolean finalRequested() {
        return Boolean.TRUE.equals(isFinal);
    }
}
