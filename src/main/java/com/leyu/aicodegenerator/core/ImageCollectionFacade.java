package com.leyu.aicodegenerator.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leyu.aicodegenerator.ai.ImageCollectionRoutingService;
import com.leyu.aicodegenerator.ai.ImageCollectionService;
import com.leyu.aicodegenerator.ai.model.ImageCollectionRoutingResult;
import com.leyu.aicodegenerator.entity.ChatHistoryOriginal;
import com.leyu.aicodegenerator.service.ChatHistoryOriginalService;
import com.leyu.aicodegenerator.utils.FluxToStringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Compile. */
@Service
@Slf4j
public class ImageCollectionFacade {

    @Resource
    private ImageCollectionRoutingService imageCollectionRoutingService;

    @Resource
    private ImageCollectionService imageCollectionService;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

/** Compile. */
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Route decision: whether image collection is needed.
     */
    public ImageCollectionRoutingResult routeImageCollection(String userPrompt, Long appId) {
        try {
            String contextualPrompt = buildContextualPrompt(userPrompt, appId);
            Flux<String> flux = imageCollectionRoutingService.routeImageCollection(contextualPrompt);
            String jsonStr = FluxToStringUtil.fluxToString(flux);
            log.info("Image collection routing result: {}", jsonStr);
            // Remove possible Markdown code-fence wrappers.
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").trim();
            }
            return objectMapper.readValue(jsonStr, ImageCollectionRoutingResult.class);
        } catch (Exception e) {
            log.error("Image collection routing failed: {}", e.getMessage());
            return ImageCollectionRoutingResult.builder()
                    .shouldCollect(false)
                    .statusMessage("")
                    .build();
        }
    }

    /**
     * Collect images and enhance the prompt.
     */
    public String collectAndEnhance(Long appId, String originalPrompt) {
        String normalizedImageList;
        try {
            String contextualPrompt = buildContextualPrompt(originalPrompt, appId);
            Flux<String> flux = imageCollectionService.collectImages(contextualPrompt);
            String rawOutput = FluxToStringUtil.fluxToString(flux);
            normalizedImageList = normalizeImageResources(rawOutput);
        } catch (Exception e) {
            log.error("Image collection failed, proceeding without images: {}", e.getMessage());
            return originalPrompt;
        }

        if (StrUtil.isBlank(normalizedImageList)) {
            log.warn("Image collection returned no valid resources, skip prompt enhancement");
            return originalPrompt;
        }

        StringBuilder enhanced = new StringBuilder(originalPrompt);
        enhanced.append("\n\n## Usable Image Resources\n");
        enhanced.append("Please use the following image resources when generating the website, ");
        enhanced.append("embedding them logically into the appropriate sections.\n");
        enhanced.append(normalizedImageList);

        log.info("Prompt enhanced with images, original length: {}, enhanced length: {}",
                originalPrompt.length(), enhanced.length());
        return enhanced.toString();
    }

/** Build Contextual Prompt. */
    private String buildContextualPrompt(String userPrompt, Long appId) {
        try {
            QueryWrapper qw = QueryWrapper.create()
                    .eq("appId", appId)
                    .eq("messageType", "user")
                    .orderBy("id", false)
                    .limit(20);
            List<ChatHistoryOriginal> recentMessages = chatHistoryOriginalService.list(qw);

            if (CollUtil.isEmpty(recentMessages)) {
                return userPrompt;
            }

            List<ChatHistoryOriginal> ordered = new ArrayList<>(recentMessages);
            Collections.reverse(ordered);

            StringBuilder sb = new StringBuilder();
            sb.append("## Conversation History:\n");
            for (ChatHistoryOriginal h : ordered) {
                Object message = ReflectUtil.getFieldValue(h, "message");
                sb.append(StrUtil.blankToDefault(message == null ? "" : message.toString(), "")).append("\n\n");
            }
            sb.append("## Current User Message:\n");
            sb.append(userPrompt);
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to load context: {}", e.getMessage());
            return userPrompt;
        }
    }

    /**
     * Normalize collector output and keep only valid image resources.
     * Any conversational text (like follow-up questions) is discarded.
     */
    private String normalizeImageResources(String rawOutput) {
        if (StrUtil.isBlank(rawOutput)) {
            return "";
        }

        String jsonText = stripCodeFence(rawOutput);
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(jsonText);
            List<String> lines = new ArrayList<>();
            collectResourceLines(root, "contentImages", "CONTENT", lines);
            collectResourceLines(root, "illustrations", "ILLUSTRATION", lines);
            collectResourceLines(root, "mermaidDiagrams", "ARCHITECTURE", lines);
            return String.join("\n", lines);
        } catch (Exception e) {
            log.warn("Image collector output is not valid JSON, rawOutput={}", rawOutput);
            return "";
        }
    }

/** Collect required image resources for the request. */
    private void collectResourceLines(ObjectNode root, String key, String tag, List<String> lines) {
        if (root == null || !root.has(key) || !(root.get(key) instanceof ArrayNode arrayNode)) {
            return;
        }

        arrayNode.forEach(item -> {
            if (!(item instanceof ObjectNode obj)) {
                return;
            }

            String url = obj.path("url").asText("").trim();
            String description = obj.path("description").asText("").trim();

            if (!isValidUrl(url)) {
                return;
            }
            if (StrUtil.isBlank(description)) {
                description = "image resource";
            }
            lines.add("- " + tag + ": " + description + " (" + url + ")");
        });
    }

/** Is Valid Url. */
    private boolean isValidUrl(String url) {
        return StrUtil.isNotBlank(url) && URL_PATTERN.matcher(url).matches();
    }

    private String stripCodeFence(String text) {
        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```\\w*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        return normalized.trim();
    }
}