package com.leyu.aicodegenerator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** AsyncExecutorConfig implementation. */
@Configuration
public class AsyncExecutorConfig implements WebMvcConfigurer {

    /**
     * Spring MVC async request executor (SSE / Flux return values)
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mvc-async-");
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        configurer.setTaskExecutor(executor);
        configurer.setDefaultTimeout(600_000); // SSE timeout: 10 minutes
    }

    /**
     * langchain4j OpenAI streaming executor.
     */
    @Bean("openAiStreamingChatModelTaskExecutor")
    public AsyncTaskExecutor openAiStreamingChatModelTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("lc4j-openai-stream-");
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}