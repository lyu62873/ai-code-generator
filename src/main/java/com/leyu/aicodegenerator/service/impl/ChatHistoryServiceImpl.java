package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.constant.UserConstant;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.service.AppService;
import com.leyu.aicodegenerator.utils.DebugSessionLogUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leyu.aicodegenerator.entity.ChatHistory;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.mapper.ChatHistoryMapper;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat history service implementation.
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;



/** Remove the target data for the given parameters. */
    @Override
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid app id");
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return remove(queryWrapper);
    }

/** Add the provided record and persist it to storage. */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "User id cannot be null");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App id cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "Message cannot be blank");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "Message type cannot be blank");

        ChatHistoryTypeEnum messageTypeEnum = ChatHistoryTypeEnum.getEnumByValue(messageType);

        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "Unsupported message type " + messageType);

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();

        try {
            boolean saved = save(chatHistory);
            // #region agent log
            DebugSessionLogUtil.log(
                    "pre-fix",
                    "H3",
                    "ChatHistoryServiceImpl.addChatMessage",
                    "chat_history_save_result",
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
                    "ChatHistoryServiceImpl.addChatMessage",
                    "chat_history_save_exception",
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

/** Build QueryWrapper filters based on the provided query request. */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (queryRequest == null) return queryWrapper;

        Long id = queryRequest.getId();
        String message = queryRequest.getMessage();
        String messageType = queryRequest.getMessageType();
        Long userId = queryRequest.getUserId();
        Long appId = queryRequest.getAppId();
        LocalDateTime lastCreateTime = queryRequest.getLastCreateTime();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId)
                .lt( "createTime", lastCreateTime, lastCreateTime != null);
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;

    }

/** List paged results based on the request and permission checks. */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "Invalid page size, not in 1-50");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App id cannot be null");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "Application not exist");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "No permission to access chat history");

        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = getQueryWrapper(queryRequest);

        return page(Page.of(1, pageSize), queryWrapper);
    }

/** Load Chat History To Memory. */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) return 0;

            historyList = historyList.reversed();

            int loadedCount = 0;
            chatMemory.clear();

            for (ChatHistory chatHistory : historyList) {
                if (ChatHistoryTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }
            }
            log.info("Successfully loaded {} chat history for AppId {}", loadedCount, appId);
            return loadedCount;
        } catch (Exception e) {
            log.error("Failed to load chat history for AppId {}, error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }


}
