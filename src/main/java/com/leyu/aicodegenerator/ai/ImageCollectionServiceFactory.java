package com.leyu.aicodegenerator.ai;

import com.leyu.aicodegenerator.langgraph4j.tools.ImageSearchTool;
import com.leyu.aicodegenerator.langgraph4j.tools.MermaidDiagramTool;
import com.leyu.aicodegenerator.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .streamingChatModel(streamingChatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool
                )
                .build();
    }
}
