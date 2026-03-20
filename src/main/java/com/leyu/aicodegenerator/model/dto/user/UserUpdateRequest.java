package com.leyu.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** UserUpdateRequest implementation. */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

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

    private static final long serialVersionUID = 1L;
}
