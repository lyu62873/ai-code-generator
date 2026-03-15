package com.leyu.aicodegenerator.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for admin to update any app.
 * Supports updating app name, cover image, and priority.
 *
 * @author Lyu
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * App id (required)
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
     * Display priority (desc order); apps with priority > 0 appear in the featured list
     */
    private Integer priority;

    @Serial
    private static final long serialVersionUID = 1L;
}
