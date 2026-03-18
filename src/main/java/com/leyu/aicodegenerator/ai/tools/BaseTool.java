package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.json.JSONObject;

public abstract class BaseTool {

    public abstract String getToolName();

    public abstract String getDisplayName();

    public String generateToolRequestResponse() {
        return String.format("\n\n[Select Tool] %s\n\n", getDisplayName());
    }

    public abstract String generateToolExecutedResult(JSONObject arguments);
}
