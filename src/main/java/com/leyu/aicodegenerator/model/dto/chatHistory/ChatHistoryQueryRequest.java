package com.leyu.aicodegenerator.model.dto.chatHistory;

import com.leyu.aicodegenerator.common.PageRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Request DTO for app chat history loading.
 * Uses cursor-based paging to load latest messages first.
 */
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     *
     */
    private Long id;

    /**
     *
     */
    private String message;

    /**
     * user/ai
     */
    private String messageType;

    /**
     *
     */
    private Long appId;

    /**
     *
     */
    private Long userId;

    /**
     * cursor
     */
    private LocalDateTime lastCreateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
