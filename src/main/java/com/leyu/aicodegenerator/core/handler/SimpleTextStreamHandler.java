package com.leyu.aicodegenerator.core.handler;

import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Handle incoming streaming chunks and update chat history (incl. tool calls). */
@Slf4j
@Component
public class SimpleTextStreamHandler {

    @Resource
    private ChatHistoryService chatHistoryService;

    public Flux<String> handle(Flux<String> origin
                                , long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return origin
                .map(chunk -> {
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(e -> {
                    String aiResponse = "AI response error: " + e.getMessage();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}
