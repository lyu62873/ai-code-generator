package com.leyu.aicodegenerator.service;

import com.mybatisflex.core.service.IService;
import com.leyu.aicodegenerator.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * Service layer.
 *
 * @author Lyu
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     *
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     *
     * @param chatHistoryOriginalList
     * @return
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     *
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     *
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
