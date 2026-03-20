package com.leyu.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** UserSelfUpdateRequest implementation. */
@Data
public class UserSelfUpdateRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
