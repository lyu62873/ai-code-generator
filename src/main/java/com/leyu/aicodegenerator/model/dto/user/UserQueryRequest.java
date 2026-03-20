package com.leyu.aicodegenerator.model.dto.user;

import com.leyu.aicodegenerator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/** UserQueryRequest implementation. */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * Name
     */
    private String userName;

    /**
     * Account
     */
    private String userAccount;

    /**
     * Profile
     */
    private String userProfile;

    /**
     * Role：user/admin
     */
    private String userRole;

    @Serial
    private static final long serialVersionUID = 1L;
}
