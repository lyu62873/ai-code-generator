package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
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
        return this.save(chatHistoryOriginal);
    }

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

        return this.saveBatch(validMessages);
    }


    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App Id cannot be null");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

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
     * 查询历史记录，考虑边缘记录类型
     * 工具调用信息必须是成对并且有序的: tool_request -> tool_result，否则就会报错！
     * 错误信息：dev.langchain4j.exception.HttpException: {"error":{"message":"Messages with role 'tool' must be a response to a preceding message with 'tool_calls'","type":"invalid_request_error","param":null,"code":"invalid_request_error"}}
     *     1. 边缘检查的意义在于当查询到的第 maxCount + 1 那条数据是 tool_result 时就丢失了一条 tool_request，导致报错
     *     2. 这里改为了按 id 倒序查询，时间戳排序可能因为相近值而不稳定，当 tool_request 和 tool_result 的顺序加载错了会导致报错（MyBatis-flex的雪花算法生成的ID是严格递增的）
     *
     * @param appId 应用ID
     * @param maxCount 最大记录数
     * @return 历史记录列表
     */
    private List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount) {
        // 1. 首先检查总记录数
        QueryWrapper countQueryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId);
        long totalCount = this.count(countQueryWrapper);

        // 2. 如果总记录数小于等于1，直接返回空列表（因为我们要跳过第1条记录）
        if (totalCount <= 1) {
            log.debug("Record number ({}) <= 1，no enough history to load", totalCount);
            return Collections.emptyList();
        }

        // 3. 计算实际可查询的最大记录数（减去要跳过的第1条记录）
        long availableCount = totalCount - 1;

        // 4. 如果总记录数小于等于 maxCount+1，则不需要检查边缘记录
        if (totalCount <= maxCount + 1) {
            log.debug("Record number ({}) <= maxCount+1 ({}), doesn't need edge check", totalCount, maxCount + 1);


            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistoryOriginal::getAppId, appId)
                    .orderBy(ChatHistoryOriginal::getId, false)
                    .limit(1, availableCount);

            return this.list(queryWrapper);
        }

        // 5. 如果总记录数大于 maxCount+1，则需要检查边缘记录
        // 查询第 maxCount+1 条记录（边缘记录）
        QueryWrapper edgeQueryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(maxCount, 1);  // 查询第 maxCount+1 条记录

        ChatHistoryOriginal edgeRecord = this.getOne(edgeQueryWrapper);

        // 6. 如果边缘记录是 TOOL_EXECUTION_RESULT 类型，则需要额外查询其前一条 TOOL_EXECUTION_REQUEST 记录
        boolean needExtraRequest = false;
        if (edgeRecord != null) {
            String edgeMessageType = edgeRecord.getMessageType();
            ChatHistoryMessageTypeEnum edgeMessageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(edgeMessageType);
            needExtraRequest = (edgeMessageTypeEnum == ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT);
        }

        // 7. 计算实际需要查询的记录数
        long actualLimit = Math.min(needExtraRequest ? maxCount + 1 : maxCount, availableCount);

        // 8. 查询历史记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(1, actualLimit);  // 查询从第2条开始的 actualLimit 条记录

        List<ChatHistoryOriginal> originalHistoryList = this.list(queryWrapper);
        if (CollUtil.isEmpty(originalHistoryList)) {
            return Collections.emptyList();
        }

        // 9. 检查是否需要调整 maxCount
        if (needExtraRequest && originalHistoryList.size() <= maxCount) {
            // 如果需要额外的 TOOL_EXECUTION_REQUEST 但没有获取到足够的记录
            log.warn("Edge record is of type TOOL_EXECUTION_RESULT, but no corresponding TOOL_EXECUTION_REQUEST was found. Decrementing maxCount by 1.");
            maxCount = Math.max(0, maxCount - 1);  // 确保 maxCount 不小于 0

            // 如果 maxCount 变为 0，则直接返回空列表
            if (maxCount == 0) {
                log.info("Adjusted maxCount is 0; no history records will be loaded.");
                return Collections.emptyList();
            }

            // 重新查询，使用调整后的 maxCount
            actualLimit = Math.min(maxCount, availableCount);
            queryWrapper = QueryWrapper.create()
                    .eq(ChatHistoryOriginal::getAppId, appId)
                    .orderBy(ChatHistoryOriginal::getId, false)
                    .limit(1, actualLimit);  // 查询从第2条开始的 actualLimit 条记录

            originalHistoryList = this.list(queryWrapper);
            if (CollUtil.isEmpty(originalHistoryList)) {
                return Collections.emptyList();
            }
        }

        return originalHistoryList;
    }

    /**
     * 将历史记录加载到内存中
     *
     * @param originalHistoryList 历史记录列表
     * @param chatMemory 聊天记忆
     * @return 加载的记录数
     */
    private int loadMessagesToMemory(List<ChatHistoryOriginal> originalHistoryList, MessageWindowChatMemory chatMemory) {
        int loadedCount = 0;
        // 遍历原始历史记录，根据类型将消息添加到记忆中
        for(ChatHistoryOriginal history : originalHistoryList) {
            // 这里需要根据消息类型进行转换，支持 AI, user, toolExecutionRequest, toolExecutionResult 4种类型
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
                    // 有些工具调用请求带有文本，有些没有
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
