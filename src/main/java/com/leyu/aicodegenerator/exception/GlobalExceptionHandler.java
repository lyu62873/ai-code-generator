package com.leyu.aicodegenerator.exception;

import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.common.BaseResponse;
import com.leyu.aicodegenerator.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** GlobalExceptionHandler implementation. */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /** businessExceptionHandler implementation. */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        if (isClientSideBusinessCode(e.getCode())) {
            logBusinessExceptionAsWarn(e);
        } else {
            log.error("业务异常，code={}，message={}", e.getCode(), e.getMessage(), e);
        }
        // Attempt to handle SSE requests
        if (handleSseError(e.getCode(), e.getMessage())) {
            return null;
        }
        // For regular requests, return a standard JSON response
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /** runtimeExceptionHandler implementation. */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        // Attempt to handle SSE requests
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), "System Error")) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "System Error");
    }

    /**
     * Handle error responses for SSE requests.
     *
     * @param errorCode    Error code.
     * @param errorMessage Error message.
     * @return true if it's an SSE request and has been handled; false otherwise.
     */
    /** handleSseError implementation. */
    private boolean handleSseError(int errorCode, String errorMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        if (request == null || response == null) {
            return false;
        }
        // Determine whether it's an SSE request (via Accept header or URL path)
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if ((accept != null && accept.contains("text/event-stream")) ||
                uri.contains("/chat/gen/code")) {
            try {
                // Set SSE response headers
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection", "keep-alive");
                // Construct SSE format for the error message
                Map<String, Object> errorData = Map.of(
                        "error", true,
                        "code", errorCode,
                        "message", errorMessage
                );
                String errorJson = JSONUtil.toJsonStr(errorData);
                // Send business-error event (avoid conflict with the default "error" event)
                String sseData = "event: business-error\ndata: " + errorJson + "\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush();
                // Send done event
                response.getWriter().write("event: done\ndata: {}\n\n");
                response.getWriter().flush();
                // Mark SSE request as handled
                return true;
            } catch (IOException ioException) {
                log.error("Failed to write SSE error response", ioException);
                // Even if writing fails, it's still considered an SSE request
                return true;
            }
        }
        return false;
    }

    private boolean isClientSideBusinessCode(int code) {
        return code == ErrorCode.NOT_LOGIN_ERROR.getCode()
                || code == ErrorCode.NO_AUTH_ERROR.getCode()
                || code == ErrorCode.PARAMS_ERROR.getCode();
    }

    private void logBusinessExceptionAsWarn(BusinessException e) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("业务请求异常，code={}，message={}", e.getCode(), e.getMessage());
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            log.warn("业务请求异常，code={}，message={}", e.getCode(), e.getMessage());
            return;
        }
        String paramSummary = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Arrays.toString(entry.getValue())
                )).toString();
        log.warn("业务请求异常，method={}，uri={}，query={}，params={}，code={}，message={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                paramSummary,
                e.getCode(),
                e.getMessage());
    }

}
