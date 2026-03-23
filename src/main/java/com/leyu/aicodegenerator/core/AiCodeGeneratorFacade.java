package com.leyu.aicodegenerator.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.ai.AiCodeGeneratorService;
import com.leyu.aicodegenerator.ai.AiCodeGeneratorServiceFactory;
import com.leyu.aicodegenerator.ai.model.HtmlCodeResult;
import com.leyu.aicodegenerator.ai.model.MultiFileCodeResult;
import com.leyu.aicodegenerator.ai.model.message.AiResponseMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.leyu.aicodegenerator.ai.model.message.ToolRequestMessage;
import com.leyu.aicodegenerator.constant.AppConstant;
import com.leyu.aicodegenerator.core.builder.VueProjectBuilder;
import com.leyu.aicodegenerator.core.parser.CodeParserExecutor;
import com.leyu.aicodegenerator.core.saver.CodeFileSaverExecutor;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.utils.DebugSessionLogUtil;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 *
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Autowired
    private VueProjectBuilder vueProjectBuilder;

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
                        long startedAt = System.currentTimeMillis();
                        // #region agent log
                        DebugSessionLogUtil.log(
                                "pre-fix",
                                "H2",
                                "AiCodeGeneratorFacade.processTokenStream",
                                "token_stream_complete_callback_started",
                                java.util.Map.of("appId", appId)
                        );
                        // #endregion
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        boolean buildSuccess = vueProjectBuilder.buildProject(projectPath);
                        // #region agent log
                        DebugSessionLogUtil.log(
                                "pre-fix",
                                "H2",
                                "AiCodeGeneratorFacade.processTokenStream",
                                "token_stream_complete_callback_finished",
                                java.util.Map.of(
                                        "appId", appId,
                                        "buildSuccess", buildSuccess,
                                        "durationMs", System.currentTimeMillis() - startedAt
                                )
                        );
                        // #endregion
                        sink.complete();
                    })
                    .onError(e -> {
                        // #region agent log
                        DebugSessionLogUtil.log(
                                "pre-fix",
                                "H2",
                                "AiCodeGeneratorFacade.processTokenStream",
                                "token_stream_on_error",
                                java.util.Map.of(
                                        "appId", appId,
                                        "errorType", e.getClass().getName(),
                                        "errorMessage", String.valueOf(e.getMessage())
                                )
                        );
                        // #endregion
                        e.printStackTrace();
                        sink.error(e);
                    })
                    .start();
        });
    }

}
