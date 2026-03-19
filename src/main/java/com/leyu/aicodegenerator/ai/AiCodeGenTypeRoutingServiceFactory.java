package com.leyu.aicodegenerator.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Slf4j
@Configuration
public class AiCodeGenTypeRoutingServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    /**
     *
     */
    @Bean
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
