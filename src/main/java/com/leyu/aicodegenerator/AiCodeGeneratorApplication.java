package com.leyu.aicodegenerator;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/** AiCodeGeneratorApplication implementation. */
@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.leyu.aicodegenerator.mapper")
public class AiCodeGeneratorApplication {

    /** main implementation. */
    public static void main(String[] args) {
        SpringApplication.run(AiCodeGeneratorApplication.class, args);
    }

}
