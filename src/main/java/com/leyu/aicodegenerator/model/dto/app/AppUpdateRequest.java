package com.leyu.aicodegenerator.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for a user to update their own app.
 * Currently only supports updating the app name.
 *
 * @author Lyu
 */
/** AppUpdateRequest implementation. */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * App id (required)
     */
    private Long id;

    /**
     * App name
     */
    private String appName;

    @Serial
    private static final long serialVersionUID = 1L;
}
