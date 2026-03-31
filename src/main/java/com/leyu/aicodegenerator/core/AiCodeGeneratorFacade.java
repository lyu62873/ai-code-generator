package com.leyu.aicodegenerator.core;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.leyu.aicodegenerator.ai.AiCodeGeneratorService;
import com.leyu.aicodegenerator.ai.AiCodeGeneratorServiceFactory;
import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import com.leyu.aicodegenerator.ai.model.message.AiResponseMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.leyu.aicodegenerator.core.parser.CodeParserExecutor;
import com.leyu.aicodegenerator.core.saver.CodeFileSaverExecutor;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 *
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    private static final int REPAIR_TIMEOUT_SECONDS = 180;
    private static final int BUILD_ERROR_MAX_CHARS = 6000;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     *
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
/** Generate output for the request (and persist/upload as needed). */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generation type is null");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "Unsupported generation type：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     *
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generation type is null");
        }

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "Unsupported generation type：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     *
     * @param codeStream
     * @param codeGenType
     * @return
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            try {
                String completeCode = codeBuilder.toString();
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("Save Success，path：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("Save Failure: {}", e.getMessage());
            }
        });
    }


/** Process Token Stream. */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse(partialResponse -> {
                AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        if (toolExecutionRequest == null || StrUtil.isBlank(toolExecutionRequest.name())) {
                            return;
                        }
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted(toolExecution -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse(response -> {
                        AiResponseMessage deploymentHintMessage = new AiResponseMessage(
                                "\n\nPlease click Deploy to create or update the webpage."
                        );
                        sink.next(JSONUtil.toJsonStr(deploymentHintMessage));
                        sink.complete();
                    })
                    .onError(e -> {
                        log.error("Vue 项目流式生成失败，appId={}，错误={}", appId, e.getMessage());
                        sink.error(e);
                    })
                    .start();
        });
    }

    public boolean repairVueProjectByBuildError(Long appId, String buildError, int round, int maxRounds) {
        if (appId == null || appId <= 0 || StrUtil.isBlank(buildError)) {
            return false;
        }

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
        String sanitizedBuildError = buildError.length() > BUILD_ERROR_MAX_CHARS
                ? buildError.substring(0, BUILD_ERROR_MAX_CHARS) + "...(truncated)"
                : buildError;
        String repairPrompt = """
                You are now in build-fix mode.
                Current round: %d/%d.
                
                The latest `npm run build` failed with this output:
                ---
                %s
                ---
                
                Requirements:
                1) Use file tools to locate and fix ONLY build-breaking issues.
                2) Prioritize syntax errors, invalid CSS selectors, broken imports/exports, malformed Vue SFC sections.
                3) Keep existing feature scope; do not add new pages/features.
                4) Apply minimal edits and stop once fixed.
                """.formatted(round, maxRounds, sanitizedBuildError);

        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicBoolean hasError = new AtomicBoolean(false);

        TokenStream repairStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, repairPrompt);
        repairStream
                .onCompleteResponse(response -> completionLatch.countDown())
                .onError(e -> {
                    hasError.set(true);
                    log.error("Vue build-fix round failed, appId={}, round={}, error={}", appId, round, e.getMessage());
                    completionLatch.countDown();
                })
                .start();

        try {
            boolean completed = completionLatch.await(REPAIR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Vue build-fix round timeout, appId={}, round={}", appId, round);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Vue build-fix round interrupted, appId={}, round={}", appId, round);
            return false;
        }
        return !hasError.get();
    }

}
