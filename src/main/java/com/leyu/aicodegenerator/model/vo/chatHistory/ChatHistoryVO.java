package com.leyu.aicodegenerator.model.vo.chatHistory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Chat history view object returned to the client.
 */
@Data
public class ChatHistoryVO implements Serializable {

    private Long id;

    private Long appId;

    private Long userId;

    private String messageType;

    private String message;

    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
