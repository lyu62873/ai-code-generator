package com.leyu.aicodegenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.leyu.aicodegenerator.ai.tools.ImageSearchTool;
import com.leyu.aicodegenerator.ai.tools.MermaidDiagramTool;
import com.leyu.aicodegenerator.ai.tools.ToolManager;
import com.leyu.aicodegenerator.ai.tools.UndrawIllustrationTool;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.service.ChatHistoryOriginalService;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;

/** Method used by this component. */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource(name = "openAiStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    @Resource(name = "reasoningStreamingChatModel")
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ToolManager toolManager;

    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener(((key, value, cause) -> {
                log.debug("AI service is removed, appId: {}, reason: {}", key, cause);
            }))
            .build();

/** Get or create (and cache) an AiCodeGeneratorService for the given appId and code-gen type. */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

/** Build the cache key used to look up cached AiCodeGeneratorService instances. */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        log.info("Creating AiCodeGeneratorService for appId: {}", appId);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(100)
                .build();

        return switch (codeGenType) {
            case VUE_PROJECT -> {
                // Vue: continue using original history to support tool call replay
                chatHistoryOriginalService.loadOriginalChatHistoryToMemory(appId, chatMemory, 50);



                AiCodeGeneratorService aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        .maxSequentialToolsInvocations(20)
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();

                yield aiCodeGeneratorService;
            }
            case MULTI_FILE, HTML -> {
                // HTML/MULTI: keep the original flow and disable tools
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

                AiCodeGeneratorService aiCodeGeneratorService = AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .build();

                yield aiCodeGeneratorService;
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported CodeGenType: " + codeGenType);
        };
    }
}