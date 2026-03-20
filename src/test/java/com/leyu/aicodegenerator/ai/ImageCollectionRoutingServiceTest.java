package com.leyu.aicodegenerator.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class ImageCollectionRoutingServiceTest {
    @Resource
    private ImageCollectionRoutingService imageCollectionRoutingService;

    @Test
    void testRouting() {
        Flux<String> flux = imageCollectionRoutingService.routeImageCollection("帮我做一个美食网站");
        String result = flux.reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .defaultIfEmpty("")
                .block();
        System.out.println(result);
        // 预期：{"shouldCollect": true, "statusMessage": "正在搜集美食相关的图片素材…"}
    }

}