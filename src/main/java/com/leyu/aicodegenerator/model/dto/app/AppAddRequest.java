package com.leyu.aicodegenerator.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for creating an app.
 *
 * @author Lyu
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * Initialization prompt (required)
     */
    private String initPrompt;

    @Serial
    private static final long serialVersionUID = 1L;
}
