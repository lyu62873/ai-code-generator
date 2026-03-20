package com.leyu.aicodegenerator.model.dto.app;

import com.leyu.aicodegenerator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for paginated app queries (user-facing).
 * Used for both "my apps" and "featured apps" list endpoints.
 * Page size is capped at 20 by the controller.
 *
 * @author Lyu
 */
/** AppQueryRequest implementation. */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {


    private Long id;

    /**
     * App name (fuzzy search)
     */
    private String appName;

    private String cover;

    private String initPrompt;

    private String codeGenType;

    private String deployKey;

    private Integer priority;

    private Long userId;

    @Serial
    private static final long serialVersionUID = 1L;
}
