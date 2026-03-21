package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyu.aicodegenerator.ai.AiCodeGenTypeRoutingService;
import com.leyu.aicodegenerator.ai.ChatGuardService;
import com.leyu.aicodegenerator.ai.model.ChatGuardResult;
import com.leyu.aicodegenerator.constant.AppConstant;
import com.leyu.aicodegenerator.core.AiCodeGeneratorFacade;
import com.leyu.aicodegenerator.core.ImageCollectionFacade;
import com.leyu.aicodegenerator.core.builder.VueProjectBuilder;
import com.leyu.aicodegenerator.core.handler.StreamHandlerExecutor;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.mapper.AppMapper;
import com.leyu.aicodegenerator.model.dto.app.AppAddRequest;
import com.leyu.aicodegenerator.model.dto.app.AppQueryRequest;
import com.leyu.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.model.vo.app.AppVO;
import com.leyu.aicodegenerator.model.vo.user.UserVO;
import com.leyu.aicodegenerator.service.*;
import com.leyu.aicodegenerator.utils.ChatGuardResponseUtils;
import com.leyu.aicodegenerator.utils.FluxToStringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.leyu.aicodegenerator.utils.FluxToCodeGenTypeUtil.fluxToCodeGenType;

/**
 * App Service Implementation
 *
 * @author Lyu
 */
/** App Service Impl. */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private final UserService userService;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;
    private final ChatHistoryService chatHistoryService;
    private final StreamHandlerExecutor streamHandlerExecutor;
    private final VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;
    @Autowired
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    @Autowired
    private ChatHistoryOriginalService  chatHistoryOriginalService;
    @Autowired
    private ImageCollectionFacade imageCollectionFacade;

    @Autowired
    private ChatGuardService chatGuardService;
    @Resource
    ObjectMapper objectMapper;

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    public AppServiceImpl(UserService userService, AiCodeGeneratorFacade aiCodeGeneratorFacade,
                          ChatHistoryService chatHistoryService, StreamHandlerExecutor streamHandlerExecutor, VueProjectBuilder vueProjectBuilder) {
        this.userService = userService;
        this.aiCodeGeneratorFacade = aiCodeGeneratorFacade;
        this.chatHistoryService = chatHistoryService;
        this.streamHandlerExecutor = streamHandlerExecutor;
        this.vueProjectBuilder = vueProjectBuilder;
    }

    /**
     * Convert an App entity to an AppVO.
     *
     * @param app the entity
     * @return AppVO
     */
/** Get App VO. */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) return null;

        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);

        Long userId = app.getUserId();

        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }

        return appVO;
    }

    /**
     * Convert a list of App entities to a list of AppVOs.
     *
     * @param appList the entity list
     * @return AppVO list
     */
/** Get App VOList. */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) return new ArrayList<>();

        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());

        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).toList();
    }

    /**
     * Build a QueryWrapper for user-facing app queries.
     * Filters by app name (fuzzy). The caller must append a userId condition
     * when querying a specific user's apps.
     *
     * @param appQueryRequest the query request
     * @return QueryWrapper
     */
/** Build QueryWrapper filters based on the provided query request. */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters unfilled");
        }

        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper qw = QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName, StrUtil.isNotBlank(appName))
                .like("cover", cover, StrUtil.isNotBlank(cover))
                .like("initPrompt", initPrompt, StrUtil.isNotBlank(initPrompt))
                .eq("codeGenType", codeGenType, StrUtil.isNotBlank(codeGenType))
                .eq("deployKey", deployKey, StrUtil.isNotBlank(deployKey))
                .eq("priority", priority, priority != null)
                .eq("userId", userId, userId != null);
        if (StrUtil.isNotBlank(sortField)) {
            qw.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            qw.orderBy("createTime", false);
        }
        return qw;
    }


/** Chat To Gen Code. */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "User message cannot be null");

        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "Application not exist");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "No permission to visit this application");

        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.SYSTEM_ERROR, "Unsupported code generation type");

        String guardJson = FluxToStringUtil.fluxToString(
                chatGuardService.guard(message, codeGenType.getValue())
        );
        ChatGuardResult guardResult = ChatGuardResponseUtils.tryParse(objectMapper, guardJson)
                .orElse(null);

        // If parsing fails or action is PASS: continue with code generation.
        // When parsing fails, let it pass to avoid model formatting jitter blocking generation.
        if (guardResult != null && StrUtil.isNotBlank(guardResult.getAction())
                && !"PASS".equalsIgnoreCase(guardResult.getAction())) {
            String reply = guardResult.getReply();
            if (reply == null || reply.isBlank()) {
                reply = "I am a web code generation assistant. Please describe your page or feature request.";
            }
            // For display: keep ChatHistory consistent with the normal flow.
            // Otherwise the optimistic UI update may hide this round in the list.
            chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), loginUser.getId());
            chatHistoryService.addChatMessage(appId, reply, ChatHistoryTypeEnum.AI.getValue(), loginUser.getId());
            // Don't write ChatHistoryOriginal: the intercepted round should not enter the code model context,
            // to avoid irrelevant content/attacks polluting subsequent generation.
            return Flux.just(reply);
        }

        // Always store the original user message in ChatHistory (for the user to see).
        chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), loginUser.getId());

        // 1) Vue: don't do image routing or collectAndEnhance; pass directly to the Vue code model + tool for self decision.
        if (CodeGenTypeEnum.VUE_PROJECT == codeGenType) {
            chatHistoryOriginalService.addOriginalChatMessage(
                    appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

            Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenType, appId);
            return streamHandlerExecutor.doExecute(contentFlux, appId, loginUser, codeGenType);
        }

        // 2) HTML / MULTI_FILE: locally simplify the decision on whether image collection is needed
        boolean shouldCollectImages = shouldCollectImagesByRule(message);
        Flux<String> statusFlux = shouldCollectImages ? Flux.just("Collecting Images...") : Flux.empty();

        Flux<String> codeGenFlux = Flux.defer(() -> {
            String promptForGeneration = message;
            if (shouldCollectImages) {
                // If collectAndEnhance fails internally, it will automatically fall back to the original prompt.
                promptForGeneration = imageCollectionFacade.collectAndEnhance(appId, message);
            }

            // ChatHistoryOriginal stores the real AI input (enhanced or original).
            chatHistoryOriginalService.addOriginalChatMessage(
                    appId, promptForGeneration, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

            Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                    promptForGeneration, codeGenType, appId);
            return streamHandlerExecutor.doExecute(contentFlux, appId, loginUser, codeGenType);
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(statusFlux, codeGenFlux);
    }

    @Override
    // @Transactional(rollbackFor = Exception.class)
    // It's acceptable that the app is deleted but chat message remains
/** Remove the target data for the given parameters. */
    public boolean removeById(Serializable id) {
        if  (id == null) return false;
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) return false;

        try {
            chatHistoryService.removeByAppId(appId);
            chatHistoryOriginalService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("remove chat message error: {}", e.getMessage());
        }

        return super.removeById(id);
    }

/** Deploy App. */
    @Override
    public String deployApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "Not logged in");

        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "Application not exist");

        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "No permission to deploy this application");

        String deployKey = app.getDeployKey();
        // generate one if deployKey is null
        if (StrUtil.isBlank(deployKey)) deployKey = RandomUtil.randomString(6);

        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Application code not exist, please generate code first");
        }

        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Build Vue project failed. Please check code and dependencies");

            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue project construction complete but dist directory does not exist");

            sourceDir = distDir;
            log.info("Vue project build success at dist directory: {}", distDir.getAbsolutePath());
        }

        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Deploy fail: " + e.getMessage());
        }

        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "Update app deploy info failed");

        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

/** Generate output for the request (and persist/upload as needed). */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        Thread.startVirtualThread(() -> {
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);

            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated =  updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "Update app screenshot info failed");
        });
    }

/** Generate output for the request (and persist/upload as needed). */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "Initialization Prompt cannot be null");

        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());

        // temp setting
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));

        CodeGenTypeEnum selectedCodeGenType = fluxToCodeGenType(aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt));
        app.setCodeGenType(selectedCodeGenType.getValue());

        boolean result = save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("App construction success, ID: {}, Type: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    /**
     * Simplified image-collection rules for HTML / MULTI_FILE:
     * - If image keywords are hit => true
     * - If only style/copy/fix keywords are hit (and no image keywords) => false
     * - Otherwise: if it looks like a "new page request" => true
     */
    private boolean shouldCollectImagesByRule(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }

        String normalized = message.toLowerCase();

        String[] imageKeywords = {
                "图片", "图", "插画", "配图", "背景图", "封面", "banner", "hero", "icon",
                "photo", "photos", "image", "images", "illustration", "illustrations",
                "replace image", "replace images", "换图", "换成", "图库"
        };

        String[] nonImageKeywords = {
                "颜色", "字体", "间距", "边距", "布局微调", "对齐", "文案", "文本", "修复", "bug",
                "color", "font", "spacing", "margin", "padding", "align", "text", "copywriting", "fix bug"
        };

        boolean hasImageHint = Arrays.stream(imageKeywords).anyMatch(normalized::contains);
        if (hasImageHint) {
            return true;
        }

        boolean hasNonImageHint = Arrays.stream(nonImageKeywords).anyMatch(normalized::contains);
        if (hasNonImageHint) {
            return false;
        }

        // Default strategy: for requests like "create a new page/site", prefer adding images.
        return isLikelyNewPageRequest(normalized);
    }

/** Is Likely New Page Request. */
    private boolean isLikelyNewPageRequest(String normalizedMessage) {
        String[] newPageKeywords = {
                "做一个", "生成一个", "创建一个", "设计一个", "新建",
                "build", "create", "generate", "design",
                "website", "web page", "landing page", "home page", "portfolio",
                "官网", "页面", "网站", "博客", "商城", "后台管理"
        };
        return Arrays.stream(newPageKeywords).anyMatch(normalizedMessage::contains);
    }


}
