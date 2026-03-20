package com.leyu.aicodegenerator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyu.aicodegenerator.ai.model.ChatGuardResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Normalizes guard model output (markdown fences, whitespace) and parses JSON.
 */
@Slf4j
public final class ChatGuardResponseUtils {

    private ChatGuardResponseUtils() {
    }

    /**
     * Strips optional ``` / ```json fences and trims, matching ImageCollectionFacade behavior.
     */
    public static String stripMarkdownFences(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").trim();
        }
        return s;
    }

    /**
     * Parses guard JSON; on failure logs and returns empty (caller should treat as PASS).
     */
    public static Optional<ChatGuardResult> tryParse(ObjectMapper objectMapper, String raw) {
        String json = stripMarkdownFences(raw);
        if (json.isBlank()) {
            log.warn("Chat guard returned empty body after strip");
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ChatGuardResult.class));
        } catch (Exception e) {
            int max = Math.min(json.length(), 500);
            log.warn("Chat guard JSON parse failed: {} | snippet: {}", e.getMessage(),
                    json.substring(0, max));
            return Optional.empty();
        }
    }
}
