package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.constant.UserConstant;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.service.AppService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leyu.aicodegenerator.entity.ChatHistory;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.mapper.AppMapper;
import com.leyu.aicodegenerator.mapper.ChatHistoryMapper;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryAdminQueryRequest;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.leyu.aicodegenerator.model.vo.chatHistory.ChatHistoryPageVO;
import com.leyu.aicodegenerator.model.vo.chatHistory.ChatHistoryVO;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chat history service implementation.
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private AppMapper appMapper;

    @Resource
    @Lazy
    private AppService appService;


    @Override
    public void saveUserMessage(Long appId, Long userId, String message) {
        saveByType(appId, userId, message, ChatHistoryTypeEnum.USER);
    }

    @Override
    public void saveAiMessage(Long appId, Long userId, String message) {
        saveByType(appId, userId, message, ChatHistoryTypeEnum.AI);
    }

    @Override
    public ChatHistoryPageVO listAppChatHistory(ChatHistoryQueryRequest queryRequest, User loginUser) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "Request cannot be null");
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Long appId = queryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid app id");

        App app = appMapper.selectOneById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "Application not found");
        ThrowUtils.throwIf(app.getUserId() == null, ErrorCode.SYSTEM_ERROR, "Application owner is missing");

        Long loginUserId = loginUser.getId();
        String loginUserRole = loginUser.getUserRole();
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUserRole);
        boolean isOwner = Objects.equals(loginUserId, app.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isOwner, ErrorCode.NO_AUTH_ERROR, "No permission to view chat history");

        int pageSize = normalizePageSize(queryRequest.getPageSize());

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("id", false)
                .limit(pageSize);

        List<ChatHistory> historyList = this.list(queryWrapper);
        List<ChatHistoryVO> historyVOList = historyList.stream()
                .map(this::toChatHistoryVO)
                .toList();

        // Return ascending order for direct chat rendering on client side.
        List<ChatHistoryVO> orderedVOList = new ArrayList<>(historyVOList);
        orderedVOList.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        ChatHistoryPageVO pageVO = new ChatHistoryPageVO();
        pageVO.setRecords(orderedVOList);

        if (CollUtil.isEmpty(historyList)) {
            pageVO.setHasMore(false);
            pageVO.setNextCursorId(null);
            return pageVO;
        }

        Long minId = historyList.stream().map(ChatHistory::getId).min(Long::compareTo).orElse(null);
        pageVO.setNextCursorId(minId);
        pageVO.setHasMore(historyList.size() >= pageSize);
        return pageVO;
    }

    @Override
    public Page<ChatHistoryVO> adminListChatHistoryByPage(ChatHistoryAdminQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "Request cannot be null");
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR, "Invalid pagination arguments");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", queryRequest.getAppId(), queryRequest.getAppId() != null)
                .eq("userId", queryRequest.getUserId(), queryRequest.getUserId() != null)
                .eq("messageType", queryRequest.getMessageType(), StrUtil.isNotBlank(queryRequest.getMessageType()))
                .orderBy("createTime", false)
                .orderBy("id", false);

        Page<ChatHistory> historyPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        List<ChatHistoryVO> historyVOList = historyPage.getRecords().stream()
                .map(this::toChatHistoryVO)
                .toList();

        Page<ChatHistoryVO> historyVOPage = new Page<>(pageNum, pageSize, historyPage.getTotalRow());
        historyVOPage.setRecords(historyVOList);
        return historyVOPage;
    }

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

    private void saveByType(Long appId, Long userId, String message, ChatHistoryTypeEnum chatHistoryTypeEnum) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid app id");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "Invalid user id");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "Message cannot be blank");
        ThrowUtils.throwIf(chatHistoryTypeEnum == null, ErrorCode.PARAMS_ERROR, "Invalid message type");

        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setUserId(userId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(chatHistoryTypeEnum.getValue());
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to save chat history"));
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private ChatHistoryVO toChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, vo);
        return vo;
    }



}
