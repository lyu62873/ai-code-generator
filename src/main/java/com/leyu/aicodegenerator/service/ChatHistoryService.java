package com.leyu.aicodegenerator.service;

import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.leyu.aicodegenerator.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * Chat history service.
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * Delete all chat history records under one app.
     */
    boolean removeByAppId(Long appId);

    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);


}
