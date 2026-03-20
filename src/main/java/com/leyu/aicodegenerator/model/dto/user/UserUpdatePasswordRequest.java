package com.leyu.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/** UserUpdatePasswordRequest implementation. */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * Old password
     */
    private String oldPassword;

    /**
     * New password
     */
    private String newPassword;

    /**
     * Confirm new password
     */
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
