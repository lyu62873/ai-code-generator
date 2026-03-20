package com.leyu.aicodegenerator.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class.
 *
 * @author Lyu
 */
/** ChatHistoryOriginal implementation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history_original")
public class ChatHistoryOriginal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    private String message;

    /**
     * user/ai/toolExecutionRequest/toolExecutionResult
     */
    @Column("messageType")
    private String messageType;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
