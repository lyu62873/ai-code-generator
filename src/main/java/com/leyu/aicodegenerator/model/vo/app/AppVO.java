package com.leyu.aicodegenerator.model.vo.app;

import com.leyu.aicodegenerator.model.vo.user.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * App view object returned to the client.
 *
 * @author Lyu
 */
@Data
public class AppVO implements Serializable {

    /**
     * App id
     */
    private Long id;

    /**
     * App name
     */
    private String appName;

    /**
     * Cover image URL
     */
    private String cover;

    /**
     * Initialization prompt
     */
    private String initPrompt;

    /**
     * Code generation type: html / multi_file
     */
    private String codeGenType;

    /**
     * Deploy key used to access the deployed app
     */
    private String deployKey;

    /**
     * Time when the app was last deployed
     */
    private LocalDateTime deployedTime;

    /**
     * Display priority; apps with priority > 0 are featured
     */
    private Integer priority;

    /**
     * Owner user id
     */
    private Long userId;

    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * Last update time
     */
    private LocalDateTime updateTime;

    /**
     * User Info
     */
    private UserVO user;

    @Serial
    private static final long serialVersionUID = 1L;
}
