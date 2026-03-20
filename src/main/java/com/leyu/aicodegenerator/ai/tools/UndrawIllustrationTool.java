package com.leyu.aicodegenerator.ai.tools;

import cn.hutool.core.util.StrUtil;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Search Illustrations. */
@Slf4j
@Component
public class UndrawIllustrationTool extends BaseTool {

    private static final String UNDRAW_API_URL = "https://undraw.co/_next/data/rxbI0cNBbVhP70ybALHAo/search/%s.json?term=%s";

/** Search Illustrations. */
    @Tool("Search for illustration images for website beautification and decoration.")
    public List<ImageResource> searchIllustrations(@P("searchIllustrations") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        String apiUrl = String.format(UNDRAW_API_URL, query, query);

        // Use try-with-resources to automatically release HTTP resources
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "Illustration");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to search illustrations：{}", e.getMessage(), e);
        }
        return imageList;
    }

/** Return the tool name exposed to the agent runtime. */
    @Override
    public String getToolName() {
        return "searchIllustrationImages";
    }

/** Return a user-facing display name for this tool. */
    @Override
    public String getDisplayName() {
        return "Search Illustration Images";
    }

/** Format the tool execution result for inclusion in chat history. */
    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String query = arguments.getStr("query");
        return String.format("[Tool Executed] %s query=%s", getDisplayName(), query);
    }
}
