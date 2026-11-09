package com.cnsHackathon.cns.global.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 app.* 설정.
 *
 * @param callTimeLimit             받기를 누른 순간부터 통화할 수 있는 시간
 * @param corsAllowedOriginPatterns 프론트 주소 패턴. Vite가 5173이 쓰이고 있으면 5174로 뜨는 것처럼
 *                                  포트가 바뀔 수 있어서 "http://localhost:*"처럼 포트를 열어 둔다.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Duration callTimeLimit, List<String> corsAllowedOriginPatterns) {

    public AppProperties {
        callTimeLimit = callTimeLimit == null ? Duration.ofMinutes(10) : callTimeLimit;
        corsAllowedOriginPatterns = corsAllowedOriginPatterns == null
                ? List.of("http://localhost:*", "http://127.0.0.1:*")
                : List.copyOf(corsAllowedOriginPatterns);
    }
}
