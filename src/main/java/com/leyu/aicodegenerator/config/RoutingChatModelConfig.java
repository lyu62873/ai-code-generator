package com.leyu.aicodegenerator.config;


import com.leyu.aicodegenerator.ai.AiCodeGenTypeRoutingService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens;
    private boolean logRequests;
    private boolean logResponses;
    private Integer timeout;


    @Bean(name = "routingChatModel")
    public ChatModel routingChatModel() {


        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }
}
