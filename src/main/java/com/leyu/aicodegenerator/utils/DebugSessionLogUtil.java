package com.leyu.aicodegenerator.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Session-scoped debug logger for Cursor debug mode.
 */
public final class DebugSessionLogUtil {

    private static final String SESSION_ID = "6a9b34";
    private static final Path LOG_PATH = Path.of("/home/leyu/projects/ai-code-generator/.cursor/debug-6a9b34.log");

    private DebugSessionLogUtil() {
    }

    public static void log(String runId,
                           String hypothesisId,
                           String location,
                           String message,
                           Map<String, Object> data) {
        try {
            JSONObject payload = new JSONObject();
            payload.set("sessionId", SESSION_ID);
            payload.set("runId", runId);
            payload.set("hypothesisId", hypothesisId);
            payload.set("location", location);
            payload.set("message", message);
            payload.set("data", data == null ? Map.of() : data);
            payload.set("timestamp", System.currentTimeMillis());
            String line = JSONUtil.toJsonStr(payload) + System.lineSeparator();
            Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Best-effort debug logging only.
        }
    }
}
