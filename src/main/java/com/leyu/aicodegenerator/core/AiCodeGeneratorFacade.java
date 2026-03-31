package com.leyu.aicodegenerator.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

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
    private static final int PRECHECK_FIX_MAX_ROUNDS = 2;
    private static final int MULTI_FILE_STAGE_MAX_RETRIES = 2;
    private static final int STAGE_STREAM_EMIT_INTERVAL_MS = 240;
    private static final int STAGE_STREAM_EMIT_MAX_BUFFER_CHARS = 900;
    private static final Pattern HTML_BLOCK_PATTERN = Pattern.compile("```html[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_BLOCK_PATTERN = Pattern.compile("```css[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_BLOCK_PATTERN = Pattern.compile("```(?:js|javascript)[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_BLOCK_PATTERN = Pattern.compile("```[^\\n]*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_JS_COMMENT_PATTERN = Pattern.compile(
            "^(?:\\s*(?:/\\*[\\s\\S]*?\\*/\\s*|//[^\\n\\r]*(?:\\r?\\n|\\r)\\s*))+"
    );

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
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
                yield generateMultiFileCodeByStagesStream(aiCodeGeneratorService, userMessage, appId);
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
                                "\n\nGeneration stream finalized. Background pre-check will continue. You can click Deploy later."
                        );
                        sink.next(JSONUtil.toJsonStr(deploymentHintMessage));
                        sink.complete();

                        // Run pre-check after stream finalization to avoid overlapping with stream persistence memory pressure.
                        Thread.ofVirtual().name("vue-post-precheck-" + appId + "-" + System.currentTimeMillis())
                                .start(() -> runVueProjectPrecheckWithRepair(appId,
                                        AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId));
                    })
                    .onError(e -> {
                        log.error("Vue 项目流式生成失败，appId={}，错误={}", appId, e.getMessage());
                        sink.error(e);
                    })
                    .start();
        });
    }

    private boolean runVueProjectPrecheckWithRepair(Long appId, String projectPath) {
        boolean checkSuccess = vueProjectBuilder.precheckProject(projectPath);
        String checkFailureDetail = vueProjectBuilder.getLastBuildFailureDetail();
        for (int round = 1; !checkSuccess && round <= PRECHECK_FIX_MAX_ROUNDS; round++) {
            if (StrUtil.isBlank(checkFailureDetail)) {
                break;
            }
            log.warn("Vue pre-check failed, triggering auto-fix round {}/{}, appId={}",
                    round, PRECHECK_FIX_MAX_ROUNDS, appId);
            boolean repairTriggered = repairVueProjectByBuildError(
                    appId,
                    checkFailureDetail,
                    round,
                    PRECHECK_FIX_MAX_ROUNDS
            );
            if (!repairTriggered) {
                break;
            }
            checkSuccess = vueProjectBuilder.precheckProject(projectPath);
            checkFailureDetail = vueProjectBuilder.getLastBuildFailureDetail();
        }
        return checkSuccess;
    }

    private Flux<String> generateMultiFileCodeByStagesStream(AiCodeGeneratorService aiCodeGeneratorService,
                                                             String userMessage,
                                                             Long appId) {
        return Flux.create(sink -> {
            try {
                sink.next("\n\n[Stage 1/3] Generating HTML structure...\n");
                String htmlRaw = collectStageStreamWithRetry(
                        "HTML",
                        appId,
                        () -> aiCodeGeneratorService.generateMultiFileHtmlStageStream(userMessage),
                        sink,
                        true
                );
                String htmlCode = extractCodeFromBlock(htmlRaw, HTML_BLOCK_PATTERN, "html");
                sink.next("\n\n[Stage 1/3] HTML complete.\n");

                sink.next("\n\n[Stage 2/3] Generating CSS styles...\n");
                String cssInput = """
                        User requirement:
                        %s

                        Final HTML (read-only context):
                        ```html
                        %s
                        ```
                        """.formatted(userMessage, htmlCode);
                String cssRaw = collectStageStreamWithRetry(
                        "CSS",
                        appId,
                        () -> aiCodeGeneratorService.generateMultiFileCssStageStream(cssInput),
                        sink,
                        true
                );
                String cssCode = extractCodeFromBlock(cssRaw, CSS_BLOCK_PATTERN, "css");
                sink.next("\n\n[Stage 2/3] CSS complete.\n");

                sink.next("\n\n[Stage 3/3] Generating JavaScript interactions...\n");
                String jsInput = """
                        User requirement:
                        %s

                        Final HTML (read-only context):
                        ```html
                        %s
                        ```

                        Final CSS (read-only context):
                        ```css
                        %s
                        ```
                        """.formatted(userMessage, htmlCode, cssCode);
                String jsRaw = collectStageStreamWithRetry(
                        "JS",
                        appId,
                        () -> aiCodeGeneratorService.generateMultiFileJsStageStream(jsInput),
                        sink,
                        true
                );
                String jsCode = extractCodeFromBlock(jsRaw, JS_BLOCK_PATTERN, "javascript");
                sink.next("\n\n[Stage 3/3] JavaScript complete.\n");

                MultiFileCodeResult result = new MultiFileCodeResult();
                result.setHtmlCode(htmlCode);
                result.setCssCode(cssCode);
                result.setJsCode(jsCode);
                File savedDir = CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
                log.info("Save Success，path：{}", savedDir.getAbsolutePath());
                sink.next("\n\n[Stage Summary] Multi-file generation complete and saved.\n");
                sink.complete();
            } catch (Exception e) {
                log.error("Generate multi-file by stages failed, appId={}, error={}", appId, e.getMessage(), e);
                try {
                    sink.next("\n\nStage generation failed, fallback to single-pass multi-file generation...\n");
                    MultiFileCodeResult fallbackResult = invokeStageWithRetry(
                            "FALLBACK_FULL",
                            appId,
                            () -> aiCodeGeneratorService.generateMultiFileCode(userMessage)
                    );
                    File savedDir = CodeFileSaverExecutor.executeSaver(fallbackResult, CodeGenTypeEnum.MULTI_FILE, appId);
                    log.info("Fallback save success，path：{}", savedDir.getAbsolutePath());
                    sink.next("""
                            ```html
                            %s
                            ```

                            ```css
                            %s
                            ```

                            ```javascript
                            %s
                            ```
                            """.formatted(
                            StrUtil.blankToDefault(fallbackResult.getHtmlCode(), ""),
                            StrUtil.blankToDefault(fallbackResult.getCssCode(), ""),
                            StrUtil.blankToDefault(fallbackResult.getJsCode(), "")
                    ));
                    sink.complete();
                } catch (Exception fallbackError) {
                    log.error("Fallback multi-file generation failed, appId={}, error={}", appId, fallbackError.getMessage(), fallbackError);
                    sink.error(fallbackError);
                }
            }
        });
    }

    private String collectStageStreamWithRetry(String stageName,
                                               Long appId,
                                               Supplier<Flux<String>> streamSupplier,
                                               reactor.core.publisher.FluxSink<String> sink,
                                               boolean streamChunksToClient) {
        Throwable lastError = null;
        int attempts = MULTI_FILE_STAGE_MAX_RETRIES + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) {
                sink.next("\n\n[Stage " + stageName + " Retry " + attempt + "/" + attempts + "]\n");
            }
            log.info("Multi-file stage stream invoke, stage={}, appId={}, attempt={}/{}", stageName, appId, attempt, attempts);

            StringBuilder stageOutputBuilder = new StringBuilder();
            StringBuilder streamPushBuffer = new StringBuilder();
            CountDownLatch stageLatch = new CountDownLatch(1);
            AtomicReference<Throwable> stageError = new AtomicReference<>();
            AtomicReference<Long> lastEmitAt = new AtomicReference<>(System.currentTimeMillis());

            try {
                streamSupplier.get().subscribe(
                        chunk -> {
                            if (chunk != null) {
                                stageOutputBuilder.append(chunk);
                                if (streamChunksToClient) {
                                    streamPushBuffer.append(chunk);
                                    long now = System.currentTimeMillis();
                                    boolean reachedInterval = now - lastEmitAt.get() >= STAGE_STREAM_EMIT_INTERVAL_MS;
                                    boolean reachedSizeLimit = streamPushBuffer.length() >= STAGE_STREAM_EMIT_MAX_BUFFER_CHARS;
                                    if (reachedInterval || reachedSizeLimit) {
                                        sink.next(streamPushBuffer.toString());
                                        streamPushBuffer.setLength(0);
                                        lastEmitAt.set(now);
                                    }
                                }
                            }
                        },
                        error -> {
                            stageError.set(error);
                            stageLatch.countDown();
                        },
                        stageLatch::countDown
                );
            } catch (RuntimeException subscribeError) {
                stageError.set(subscribeError);
                stageLatch.countDown();
            }

            boolean stageCompleted;
            try {
                stageCompleted = stageLatch.await(240, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Stage stream interrupted, stage=" + stageName + ", appId=" + appId);
            }

            if (!stageCompleted) {
                stageError.set(new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Stage stream timeout, stage=" + stageName + ", appId=" + appId));
            }

            if (stageError.get() == null) {
                if (streamChunksToClient && streamPushBuffer.length() > 0) {
                    sink.next(streamPushBuffer.toString());
                }
                return stageOutputBuilder.toString();
            }

            lastError = stageError.get();
            log.warn("Multi-file stage stream failed, stage={}, appId={}, attempt={}/{}, error={}",
                    stageName, appId, attempt, attempts, lastError.getMessage());

            if (attempt < attempts) {
                long backoffMillis = 300L * attempt;
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "Stage retry interrupted, stage=" + stageName + ", appId=" + appId);
                }
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                "Stage failed after retries, stage=" + stageName + ", appId=" + appId + ", error="
                        + (lastError == null ? "unknown" : lastError.getMessage()));
    }

    private <T> T invokeStageWithRetry(String stageName, Long appId, Supplier<T> supplier) {
        RuntimeException lastException = null;
        int attempts = MULTI_FILE_STAGE_MAX_RETRIES + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                log.info("Multi-file stage invoke, stage={}, appId={}, attempt={}/{}", stageName, appId, attempt, attempts);
                return supplier.get();
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("Multi-file stage failed, stage={}, appId={}, attempt={}/{}, error={}",
                        stageName, appId, attempt, attempts, e.getMessage());
                if (attempt < attempts) {
                    long backoffMillis = 300L * attempt;
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                                "Stage retry interrupted, stage=" + stageName + ", appId=" + appId);
                    }
                }
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                "Stage failed after retries, stage=" + stageName + ", appId=" + appId + ", error="
                        + (lastException == null ? "unknown" : lastException.getMessage()));
    }

    private String extractCodeFromBlock(String raw, Pattern pattern, String language) {
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generated " + language + " content is empty");
        }
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            String content = matcher.group(1);
            if (StrUtil.isBlank(content)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generated " + language + " code block is empty");
            }
            String normalizedContent = content.trim();
            if ("javascript".equalsIgnoreCase(language) || "js".equalsIgnoreCase(language)) {
                normalizedContent = stripLeadingJsComments(normalizedContent);
            }
            return normalizedContent;
        }
        // Some model responses occasionally omit/alter the language marker.
        // Fall back to the first fenced code block to avoid saving markdown fences as source code.
        Matcher genericMatcher = GENERIC_BLOCK_PATTERN.matcher(raw);
        if (genericMatcher.find()) {
            String genericContent = genericMatcher.group(1);
            if (StrUtil.isBlank(genericContent)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generated " + language + " code block is empty");
            }
            log.warn("Generated {} code block language marker mismatch, using generic fenced block fallback", language);
            return genericContent.trim();
        }
        String trimmed = raw.trim();
        if (StrUtil.isBlank(trimmed)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generated " + language + " content is empty");
        }
        if ("javascript".equalsIgnoreCase(language) || "js".equalsIgnoreCase(language)) {
            return stripLeadingJsComments(trimmed);
        }
        return trimmed;
    }

    private String stripLeadingJsComments(String jsCode) {
        if (StrUtil.isBlank(jsCode)) {
            return jsCode;
        }
        String sanitized = LEADING_JS_COMMENT_PATTERN.matcher(jsCode).replaceFirst("");
        return sanitized.stripLeading();
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
                .onPartialResponse(partialResponse -> {
                    // Required by TokenStream contract; repair flow does not need to stream these chunks.
                })
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
