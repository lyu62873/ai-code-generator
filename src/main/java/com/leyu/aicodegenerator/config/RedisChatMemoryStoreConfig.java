package com.leyu.aicodegenerator.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RedisChatMemoryStoreConfig implementation. */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;
    private Integer port;
    private String password;
    private long ttl;

    /** redisChatMemoryStore implementation. */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {

        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl);

        if (StringUtils.isNotBlank(password)) {
            builder.user("default");
        }

        return builder.build();
    }
}
