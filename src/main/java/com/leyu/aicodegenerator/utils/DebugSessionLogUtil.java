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
    private static final Path PRIMARY_LOG_PATH = Path.of("/home/leyu/projects/ai-code-generator/.cursor/debug-6a9b34.log");

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
            writeToPath(PRIMARY_LOG_PATH, line);
        } catch (Exception ignored) {
            // Best-effort debug logging only.
        }
    }

    private static void writeToPath(Path path, String line) throws Exception {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception primaryEx) {
            Path fallbackPath = Path.of(System.getProperty("user.dir"), ".cursor", "debug-6a9b34.log");
            Path fallbackParent = fallbackPath.getParent();
            if (fallbackParent != null && !Files.exists(fallbackParent)) {
                Files.createDirectories(fallbackParent);
            }
            Files.writeString(fallbackPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
}
