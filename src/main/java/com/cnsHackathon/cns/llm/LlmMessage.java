package com.cnsHackathon.cns.llm;

public record LlmMessage(Role role, String text) {

    public enum Role {
        USER("user"),
        MODEL("model");

        private final String apiName;

        Role(String apiName) {
            this.apiName = apiName;
        }

        public String apiName() {
            return apiName;
        }
    }

    public static LlmMessage user(String text) {
        return new LlmMessage(Role.USER, text);
    }

    public static LlmMessage model(String text) {
        return new LlmMessage(Role.MODEL, text);
    }
}
