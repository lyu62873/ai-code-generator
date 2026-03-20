package com.leyu.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** UserAddRequest implementation. */
@Data
public class UserAddRequest implements Serializable {

    /**
     * Name
     */
    private String userName;

    /**
     * Account
     */
    private String userAccount;

    /**
     * Avatar
     */
    private String userAvatar;

    /**
     * Profile
     */
    private String userProfile;

    /**
     * Role: user, admin
     */
    private String userRole;

    @Serial
    private static final long serialVersionUID = 1L;
}

