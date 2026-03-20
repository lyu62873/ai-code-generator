package com.leyu.aicodegenerator.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Image Collection Routing Service. */
@Configuration
public class ImageCollectionRoutingServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

/** Image Collection Routing Service. */
    @Bean
    public ImageCollectionRoutingService imageCollectionRoutingService() {
        return AiServices.builder(ImageCollectionRoutingService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}