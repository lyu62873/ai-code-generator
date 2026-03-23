package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.leyu.aicodegenerator.utils.DebugSessionLogUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leyu.aicodegenerator.entity.ChatHistoryOriginal;
import com.leyu.aicodegenerator.mapper.ChatHistoryOriginalMapper;
import com.leyu.aicodegenerator.service.ChatHistoryOriginalService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lyu
 */
/** Add the provided record and persist it to storage. */
@Service
@Slf4j
public class ChatHistoryOriginalServiceImpl extends ServiceImpl<ChatHistoryOriginalMapper, ChatHistoryOriginal>  implements ChatHistoryOriginalService{

    @Override
    public boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App Id cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "Message cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "MessageType cannot be null");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "User Id cannot be null");
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.SYSTEM_ERROR, "Unsupported message Type: " + messageType);
        ChatHistoryOriginal chatHistoryOriginal = ChatHistoryOriginal.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        try {
            boolean saved = this.save(chatHistoryOriginal);
            // #region agent log
            DebugSessionLogUtil.log(
                    "pre-fix",
                    "H3",
                    "ChatHistoryOriginalServiceImpl.addOriginalChatMessage",
                    "chat_history_original_save_result",
                    java.util.Map.of(
                            "appId", appId,
                            "userId", userId,
                            "messageType", messageType,
                            "messageLength", message.length(),
                            "saved", saved
                    )
            );
            // #endregion
            return saved;
        } catch (Exception e) {
            // #region agent log
            DebugSessionLogUtil.log(
                    "pre-fix",
                    "H3",
                    "ChatHistoryOriginalServiceImpl.addOriginalChatMessage",
                    "chat_history_original_save_exception",
                    java.util.Map.of(
                            "appId", appId,
                            "userId", userId,
                            "messageType", messageType,
                            "messageLength", message.length(),
                            "errorType", e.getClass().getName(),
                            "errorMessage", String.valueOf(e.getMessage())
                    )
            );
            // #endregion
            throw e;
        }
    }

/** Add the provided record and persist it to storage. */
    @Override
    public boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList) {
        ThrowUtils.throwIf(chatHistoryOriginalList == null || chatHistoryOriginalList.isEmpty(),
                ErrorCode.PARAMS_ERROR, "Messages cannot be null or empty");

        List<ChatHistoryOriginal> validMessages = chatHistoryOriginalList.stream()
                .filter(chatHistory -> {
                    ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(chatHistory.getMessageType());
                    if (messageTypeEnum == null) {
                        log.error("Unsupported message type: {}", chatHistory.getMessageType());
                        return false; // filter out useless message
                    }
                    return true; // keep useful message
                })
                .collect(Collectors.toList());

        if (validMessages.isEmpty()) {
            return false;
        }

        try {
            boolean saved = this.saveBatch(validMessages);
            // #region agent log
            DebugSessionLogUtil.log(
                    "pre-fix",
                    "H3",
                    "ChatHistoryOriginalServiceImpl.addOriginalChatMessageBatch",
                    "chat_history_original_batch_save_result",
                    java.util.Map.of(
                            "batchSize", validMessages.size(),
                            "saved", saved
                    )
            );
            // #endregion
            return saved;
        } catch (Exception e) {
            // #region agent log
            DebugSessionLogUtil.log(
                    "pre-fix",
                    "H3",
                    "ChatHistoryOriginalServiceImpl.addOriginalChatMessageBatch",
                    "chat_history_original_batch_save_exception",
                    java.util.Map.of(
                            "batchSize", validMessages.size(),
                            "errorType", e.getClass().getName(),
                            "errorMessage", String.valueOf(e.getMessage())
                    )
            );
            // #endregion
            throw e;
        }
    }


/** Remove the target data for the given parameters. */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App Id cannot be null");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

/** Load Original Chat History To Memory. */
    @Override
    public int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try{
            List<ChatHistoryOriginal> originalHistoryList = queryHistoryWithEdgeCheck(appId, maxCount);
            if (CollUtil.isEmpty(originalHistoryList)) {
                return 0;
            }

            originalHistoryList = originalHistoryList.reversed();

            chatMemory.clear();

            int loadedCount = loadMessagesToMemory(originalHistoryList, chatMemory);

            log.info("Successfully load {} messages to app {}", loadedCount, appId);
            return loadedCount;
        } catch (Exception e) {
            log.error("Failed to load history，appId: {}，error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Query chat history records, taking edge record types into account.
     * Tool call info must be paired and ordered: tool_request -> tool_result; otherwise it will throw an error!
     * Error message: dev.langchain4j.exception.HttpException: {"error":{"message":"Messages with role 'tool' must be a response to a preceding message with 'tool_calls'","type":"invalid_request_error","param":null,"code":"invalid_request_error"}}
     *     1. Edge checking matters because when the record at maxCount + 1 is tool_result, one tool_request is missing, which causes the error.
     *     2. This was changed to query in descending order by id. Timestamp ordering may be unstable when values are close.
     *        If tool_request and tool_result are loaded in the wrong order, it will also cause an error
     *        (MyBatis-flex snowflake IDs are strictly increasing).
     *
     * @param appId Application ID
     * @param maxCount Maximum record count
     * @return History record list
     */
/** Query History With Edge Check. */
    private List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount) {
        // 1. First, check the total record count
        QueryWrapper countQueryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId);
        long totalCount = this.count(countQueryWrapper);

        // 2. If totalCount <= 1, return an empty list (because we skip the first record)
        if (totalCount <= 1) {
            log.debug("Record number ({}) <= 1，no enough history to load", totalCount);
            return Collections.emptyList();
        }

        // 3. Calculate the actual maximum number of records we can query (minus the first record we skip)
        long availableCount = totalCount - 1;

        // 4. If totalCount <= maxCount+1, edge checking is not needed
        if (totalCount <= maxCount + 1) {
            log.debug("Record number ({}) <= maxCount+1 ({}), doesn't need edge check", totalCount, maxCount + 1);


            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistoryOriginal::getAppId, appId)
                    .orderBy(ChatHistoryOriginal::getId, false)
                    .limit(1, availableCount);

            return this.list(queryWrapper);
        }

        // 5. If totalCount > maxCount+1, we need to check the edge records
        // Query the (maxCount+1)-th record (edge record)
        QueryWrapper edgeQueryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(maxCount, 1);  // Query the (maxCount+1)-th record

        ChatHistoryOriginal edgeRecord = this.getOne(edgeQueryWrapper);

        // 6. If the edge record is TOOL_EXECUTION_RESULT, query the preceding TOOL_EXECUTION_REQUEST record as well
        boolean needExtraRequest = false;
        if (edgeRecord != null) {
            String edgeMessageType = edgeRecord.getMessageType();
            ChatHistoryMessageTypeEnum edgeMessageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(edgeMessageType);
            needExtraRequest = (edgeMessageTypeEnum == ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT);
        }

        // 7. Calculate the actual number of records to query
        long actualLimit = Math.min(needExtraRequest ? maxCount + 1 : maxCount, availableCount);

        // 8. Query history records
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(1, actualLimit);  // Query actualLimit records starting from the 2nd record

        List<ChatHistoryOriginal> originalHistoryList = this.list(queryWrapper);
        if (CollUtil.isEmpty(originalHistoryList)) {
            return Collections.emptyList();
        }

        // 9. Check whether we need to adjust maxCount
        if (needExtraRequest && originalHistoryList.size() <= maxCount) {
            // If we needed an extra TOOL_EXECUTION_REQUEST but didn't get enough records
            log.warn("Edge record is of type TOOL_EXECUTION_RESULT, but no corresponding TOOL_EXECUTION_REQUEST was found. Decrementing maxCount by 1.");
            maxCount = Math.max(0, maxCount - 1);  // Ensure maxCount is not less than 0

            // If maxCount becomes 0, return an empty list
            if (maxCount == 0) {
                log.info("Adjusted maxCount is 0; no history records will be loaded.");
                return Collections.emptyList();
            }

            // Re-query using the adjusted maxCount
            actualLimit = Math.min(maxCount, availableCount);
            queryWrapper = QueryWrapper.create()
                    .eq(ChatHistoryOriginal::getAppId, appId)
                    .orderBy(ChatHistoryOriginal::getId, false)
                    .limit(1, actualLimit);  // Query actualLimit records starting from the 2nd record

            originalHistoryList = this.list(queryWrapper);
            if (CollUtil.isEmpty(originalHistoryList)) {
                return Collections.emptyList();
            }
        }

        return originalHistoryList;
    }

    /**
     * Load history records into memory.
     *
     * @param originalHistoryList History record list
     * @param chatMemory Chat memory
     * @return Loaded record count
     */
/** Load Messages To Memory. */
    private int loadMessagesToMemory(List<ChatHistoryOriginal> originalHistoryList, MessageWindowChatMemory chatMemory) {
        int loadedCount = 0;
        // Iterate original history records and add messages to memory by type
        for(ChatHistoryOriginal history : originalHistoryList) {
            // Convert based on message type; supported types: AI, user, toolExecutionRequest, toolExecutionResult
            String messageType = history.getMessageType();
            ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
            switch (messageTypeEnum) {
                case USER -> {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                }
                case AI -> {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
                case TOOL_EXECUTION_REQUEST -> {
                    ToolRequestMessage toolRequestMessage = JSONUtil.toBean(history.getMessage(), ToolRequestMessage.class);
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .id(toolRequestMessage.getId())
                            .name(toolRequestMessage.getName())
                            .arguments(toolRequestMessage.getArguments())
                            .build();
                    // Some tool request messages include text; others don't
                    if (toolRequestMessage.getText().isEmpty()) {
                        chatMemory.add(AiMessage.from(List.of(toolExecutionRequest)));
                    } else {
                        chatMemory.add(AiMessage.from(toolRequestMessage.getText(), List.of(toolExecutionRequest)));
                    }
                    loadedCount++;
                }
                case TOOL_EXECUTION_RESULT -> {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(history.getMessage(), ToolExecutedMessage.class);
                    String id = toolExecutedMessage.getId();
                    String toolName = toolExecutedMessage.getName();
                    String toolExecutionResult = toolExecutedMessage.getResult();
                    chatMemory.add(ToolExecutionResultMessage.from(id, toolName, toolExecutionResult));
                    loadedCount++;
                }
                case null -> log.error("Unknown Message Type: {}", messageType);
            }
        }
        return loadedCount;
    }
}
