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
public class ImageCollectionServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;


    /**
     * Create image collection AI service
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .streamingChatModel(streamingChatModel)
                .tools()
                .build();
    }
}
