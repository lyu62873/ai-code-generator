package com.leyu.aicodegenerator.model.dto.chatHistory;

import com.leyu.aicodegenerator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for admin chat history management query.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryAdminQueryRequest extends PageRequest implements Serializable {

    private Long appId;

    private Long userId;

    private String messageType;

    @Serial
    private static final long serialVersionUID = 1L;
}
