package com.cnsHackathon.cns.llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/**
 * resources/prompts 아래의 프롬프트 파일. {{name}} 자리에 값을 채운다.
 * 프롬프트 문구는 자바 코드를 고치지 않고 이 파일만 고쳐서 튜닝한다.
 */
public final class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final String name;
    private final String text;

    PromptTemplate(String name, String text) {
        this.name = name;
        this.text = text;
    }

    public static PromptTemplate load(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return new PromptTemplate(classpathLocation, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일을 읽지 못했어요: " + classpathLocation, e);
        }
    }

    public String render(Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + "의 {{" + key + "}} 값이 없어요.");
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
