package com.cnsHackathon.cns.llm;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini responseJsonSchema에 넣을 JSON Schema를 만드는 도우미.
 * 객체의 모든 속성은 필수로 둔다. 값이 없을 때는 null 대신 0, "", "NONE" 같은 약속된 값을 쓴다.
 */
public final class JsonSchemas {

    private JsonSchemas() {
    }

    public static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> string(String description) {
        return typed("string", description);
    }

    public static Map<String, Object> integer(String description) {
        return typed("integer", description);
    }

    public static Map<String, Object> bool(String description) {
        return typed("boolean", description);
    }

    public static Map<String, Object> enumOf(Collection<String> values, String description) {
        Map<String, Object> schema = typed("string", description);
        schema.put("enum", List.copyOf(values));
        return schema;
    }

    public static Map<String, Object> enumOf(Enum<?>[] values, String description) {
        return enumOf(Arrays.stream(values).map(Enum::name).toList(), description);
    }

    public static Map<String, Object> arrayOf(Map<String, Object> items, String description) {
        Map<String, Object> schema = typed("array", description);
        schema.put("items", items);
        return schema;
    }

    /** 순서를 지키는 속성 목록: props("name", string(...), "age", integer(...)) */
    public static Map<String, Object> props(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("키와 값을 짝지어 넣어야 해요.");
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }

    private static Map<String, Object> typed(String type, String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        if (description != null) {
            schema.put("description", description);
        }
        return schema;
    }
}
