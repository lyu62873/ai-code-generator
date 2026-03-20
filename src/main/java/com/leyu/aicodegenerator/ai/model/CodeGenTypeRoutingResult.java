package com.leyu.aicodegenerator.ai.model;

import jdk.jfr.Description;
import lombok.Data;

/** Method used by this component. */
@Description("Result of AI routing Code Generation type")
@Data
public class CodeGenTypeRoutingResult {

    @Description("Code Type")
    private String CodeGenType;
}
