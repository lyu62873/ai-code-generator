package com.leyu.aicodegenerator.model.vo.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * Account
     */
    private String userAccount;

    /**
     * Name
     */
    private String userName;

    /**
     * Avatar
     */
    private String userAvatar;

    /**
     * Profile
     */
    private String userProfile;

    /**
     * Role：user/admin
     */
    private String userRole;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
