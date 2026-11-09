package com.cnsHackathon.cns.global.support;

import java.time.Duration;

public final class Texts {

    private Texts() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** 남은 시간을 초 단위로 올림한다. 0.2초 남았으면 1초로 보여 준다. */
    public static int ceilSeconds(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return 0;
        }
        return (int) ((duration.toMillis() + 999) / 1000);
    }

    /** 4분 10초 형식. */
    public static String minutesAndSeconds(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        return (seconds / 60) + "분 " + (seconds % 60) + "초";
    }
}
