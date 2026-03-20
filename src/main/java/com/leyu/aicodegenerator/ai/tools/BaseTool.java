package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.json.JSONObject;

public abstract class BaseTool {

/** Return the tool name exposed to the agent runtime. */
    public abstract String getToolName();

    public abstract String getDisplayName();

    public String generateToolRequestResponse() {
        return String.format("\n\n[Select Tool] %s\n\n", getDisplayName());
    }

/** Format the tool execution result for inclusion in chat history. */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
