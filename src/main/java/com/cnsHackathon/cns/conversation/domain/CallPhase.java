package com.cnsHackathon.cns.conversation.domain;

import java.time.Duration;
import java.util.StringJoiner;

/**
 * 통화 경과 시간에 따른 사기범의 진행 단계. 시작 시각은 기준값이라 PoC 결과를 보고 조정한다.
 */
public enum CallPhase {

    APPROACH("접근", 0, "신분을 밝히고 전화한 이유를 제시한다"),
    TRUST("신뢰 형성", 2, "근거를 대고 사용자 정보를 아는 척하며 믿게 만든다"),
    PRESSURE("압박", 4, "긴급성과 불이익을 강조한다"),
    DEMAND("요구", 6, "송금, 앱 설치, 정보 제공 같은 최종 요구를 한다"),
    CLOSING("마무리", 9, "요구를 매듭짓고 통화를 정리한다");

    private static final int CALL_MINUTES = 10;

    private final String label;
    private final int startMinute;
    private final String goal;

    CallPhase(String label, int startMinute, String goal) {
        this.label = label;
        this.startMinute = startMinute;
        this.goal = goal;
    }

    public String label() {
        return label;
    }

    public static CallPhase at(Duration elapsed) {
        CallPhase current = APPROACH;
        for (CallPhase phase : values()) {
            if (elapsed.toSeconds() >= phase.startMinute * 60L) {
                current = phase;
            }
        }
        return current;
    }

    /** 프롬프트에 넣는 단계 계획. 예: "- 0~2분 접근(APPROACH): ..." */
    public static String plan() {
        StringJoiner lines = new StringJoiner("\n");
        CallPhase[] phases = values();
        for (int i = 0; i < phases.length; i++) {
            int end = i + 1 < phases.length ? phases[i + 1].startMinute : CALL_MINUTES;
            CallPhase phase = phases[i];
            lines.add("- " + phase.startMinute + "~" + end + "분 " + phase.label + "(" + phase.name() + "): " + phase.goal);
        }
        return lines.toString();
    }
}
