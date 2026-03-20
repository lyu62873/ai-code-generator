package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.entity.ImageResource;
import com.leyu.aicodegenerator.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Search Content Images. */
@Slf4j
@Component
public class ImageSearchTool extends BaseTool {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api-key}")
    private String pexelsApiKey;

/** Search Content Images. */
    @Tool("Search content related images for website to display")
    public List<ImageResource> searchContentImages(@P("Search Keyword") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        // Call the API and ensure resources are released
        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", pexelsApiKey)
                .form("query", query)
                .form("per_page", searchCount)
                .form("page", 1)
                .execute()) {
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                JSONArray photos = result.getJSONArray("photos");
                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    JSONObject src = photo.getJSONObject("src");
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(photo.getStr("alt", query))
                            .url(src.getStr("medium"))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Pexels API execution failed: {}", e.getMessage(), e);
        }
        return imageList;
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "searchContentImages";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "Search Content Images";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String query = arguments.getStr("query");
        return String.format("[Tool Executed] %s query=%s", getDisplayName(), query);
    }
}
