package com.cnsHackathon.cns.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Vite 프록시를 거쳐도 브라우저가 보낸 Origin 헤더(예: http://localhost:5174)가 그대로 넘어온다.
     * 허용 목록에 없는 Origin이면 Spring이 403 "Invalid CORS request"로 막으므로 패턴으로 연다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(appProperties.corsAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST");
    }
}
