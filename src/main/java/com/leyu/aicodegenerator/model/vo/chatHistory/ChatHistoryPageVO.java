package com.leyu.aicodegenerator.model.vo.chatHistory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Cursor paging result for app chat history.
 */
@Data
public class ChatHistoryPageVO implements Serializable {

    /**
     * Current page records in ascending create order.
     */
    private List<ChatHistoryVO> records;

    /**
     * Cursor id for loading older data.
     * Null means no more history.
     */
    private Long nextCursorId;

    /**
     * Whether older history exists.
     */
    private Boolean hasMore;

    @Serial
    private static final long serialVersionUID = 1L;
}
