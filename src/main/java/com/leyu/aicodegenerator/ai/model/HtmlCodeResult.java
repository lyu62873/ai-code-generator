package com.leyu.aicodegenerator.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Description("Result of generating HTML codes")
@Data
public class HtmlCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("Description of generated codes")
    private String description;
}
