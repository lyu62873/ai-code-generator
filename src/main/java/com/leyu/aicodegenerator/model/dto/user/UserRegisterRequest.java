package com.leyu.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** UserRegisterRequest implementation. */
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * Account
     */
    private String userAccount;

    /**
     * Password
     */
    private String userPassword;

    /**
     * Check password
     */
    private String checkPassword;
}
