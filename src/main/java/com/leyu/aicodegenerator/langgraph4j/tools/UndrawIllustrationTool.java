package com.leyu.aicodegenerator.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.langgraph4j.state.ImageResource;
import com.leyu.aicodegenerator.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_API_URL = "https://undraw.co/_next/data/rxbI0cNBbVhP70ybALHAo/search/%s.json?term=%s";

    @Tool("Search for illustration images for website beautification and decoration.")
    public List<ImageResource> searchIllustrations(@P("searchIllustrations") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        String apiUrl = String.format(UNDRAW_API_URL, query, query);

        // 使用 try-with-resources 自动释放 HTTP 资源
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
}
