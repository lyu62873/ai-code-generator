package com.leyu.aicodegenerator.controller;

import com.leyu.aicodegenerator.annotation.AuthCheck;
import com.leyu.aicodegenerator.common.BaseResponse;
import com.leyu.aicodegenerator.common.ResultUtils;
import com.leyu.aicodegenerator.constant.UserConstant;
import com.leyu.aicodegenerator.entity.ChatHistory;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.leyu.aicodegenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * Chat history controller.
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserService userService;

    /**
     *
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false)LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> res = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);

        return ResultUtils.success(res);

    }

    /**
     * Admin endpoint for reviewing all apps chat history.
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> adminListChatHistoryByPage(
            @RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(queryRequest);
        Page<ChatHistory> res = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);

        return ResultUtils.success(res);
    }

}
