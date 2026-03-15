package com.leyu.aicodegenerator.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Description("Result of generating multiple codes")
@Data
public class MultiFileCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("CSS code")
    private String cssCode;

    @Description("JS code")
    private String jsCode;

    @Description("Description of generated codes")
    private String description;
}
