package com.leyu.aicodegenerator.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** AppDeployRequest implementation. */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * Application ID
     */
    private Long appId;

    @Serial
    private static final long serialVersionUID = 1L;
}
