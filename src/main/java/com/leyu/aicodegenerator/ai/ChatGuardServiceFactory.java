package com.leyu.aicodegenerator.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chat Guard Service. */
@Configuration
public class ChatGuardServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

/** Chat Guard Service. */
    @Bean
    public ChatGuardService chatGuardService() {
        return AiServices.builder(ChatGuardService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
