package com.cnsHackathon.cns.global.support;

/**
 * 화면에 보이는 한글 이름(label)을 가진 enum.
 * 프론트는 "30대", "자영업자"처럼 한글 값을 보내므로 한글 이름과 enum 이름을 모두 받아 준다.
 */
public interface Labeled {

    String label();

    static <E extends Enum<E> & Labeled> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.label().equals(trimmed) || constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(type.getSimpleName() + "에 없는 값이에요: " + value);
    }
}
