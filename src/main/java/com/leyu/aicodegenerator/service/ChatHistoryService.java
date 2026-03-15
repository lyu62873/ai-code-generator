package com.leyu.aicodegenerator.service;

import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryAdminQueryRequest;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.leyu.aicodegenerator.model.vo.chatHistory.ChatHistoryPageVO;
import com.leyu.aicodegenerator.model.vo.chatHistory.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.leyu.aicodegenerator.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * Chat history service.
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * Save a user message record.
     */
    void saveUserMessage(Long appId, Long userId, String message);

    /**
     * Save a successful AI message record.
     */
    void saveAiMessage(Long appId, Long userId, String message);

    /**
     * Query app chat history by cursor pagination.
     * Only app owner or admin can query.
     */
    ChatHistoryPageVO listAppChatHistory(ChatHistoryQueryRequest queryRequest, User loginUser);

    /**
     * Query all chat history for admin management.
     */
    Page<ChatHistoryVO> adminListChatHistoryByPage(ChatHistoryAdminQueryRequest queryRequest);

    /**
     * Delete all chat history records under one app.
     */
    boolean removeByAppId(Long appId);

    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);
}
