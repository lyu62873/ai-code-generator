package com.leyu.aicodegenerator.ai;

import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.ai.model.message.AiResponseMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.apache.el.parser.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionService imageCollectionService;

    @Test
    void testTechWebsiteImageCollection() throws InterruptedException {
        StringBuilder partialSb = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errRef = new AtomicReference<>();
        AtomicReference<String> finalTextRef = new AtomicReference<>("");

        //Flux<String> stringFlux = processTokenStream(imageCollectionService.collectImages("Create a bio page with many cute dogs"));

        String raw = imageCollectionService.collectImages("Create a bio page with many cute dogs")
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .block();


        System.out.println("最终输出: " + raw);
        Assertions.assertNull(errRef.get(), "stream error");
        Assertions.assertFalse(raw == null || raw.isBlank(), "no output text");
        System.out.println("最终输出: " + raw);
    }

    @Test
    void testEcommerceWebsiteImageCollection() {
        Flux<String> result = imageCollectionService.collectImages("创建一个电商购物网站，需要展示商品和品牌形象");
        Assertions.assertNotNull(result);
        System.out.println("电商网站收集到的图片: " + result);
    }


    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted(toolExecution -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse(response -> {
                        sink.complete();
                    })
                    .onError(e -> {
                        e.printStackTrace();
                        sink.error(e);
                    })
                    .start();
        });
    }
}
