package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.constant.UserConstant;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.service.AppService;
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
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Chat history service implementation.
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;



    @Override
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid app id");
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return remove(queryWrapper);
    }

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

        return save(chatHistory);
    }

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



}
