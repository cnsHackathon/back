package com.cnsHackathon.cns.global.config;

import com.cnsHackathon.cns.llm.GeminiProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableConfigurationProperties({AppProperties.class, GeminiProperties.class})
public class AppConfig {

    /** 시간 계산은 모두 이 Clock을 쓴다. 테스트에서 고정 시각으로 바꿀 수 있다. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** AI 호출처럼 오래 걸리는 작업을 트랜잭션 밖에 두고, 저장만 짧게 묶을 때 쓴다. */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
