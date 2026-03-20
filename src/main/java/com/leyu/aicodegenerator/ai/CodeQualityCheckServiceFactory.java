package com.leyu.aicodegenerator.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Generate output for the request (and persist/upload as needed). */
@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    /**
     * Create code quality check AI service.
     */
    @Bean
    public CodeQualityCheckService createCodeQualityCheckService() {
        return AiServices.builder(CodeQualityCheckService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
