package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.constant.AppConstant;
import com.leyu.aicodegenerator.core.AiCodeGeneratorFacade;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.mapper.AppMapper;
import com.leyu.aicodegenerator.model.dto.app.AppQueryRequest;
import com.leyu.aicodegenerator.model.enums.ChatHistoryTypeEnum;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.model.vo.app.AppVO;
import com.leyu.aicodegenerator.model.vo.user.UserVO;
import com.leyu.aicodegenerator.service.AppService;
import com.leyu.aicodegenerator.service.ChatHistoryService;
import com.leyu.aicodegenerator.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * App Service Implementation
 *
 * @author Lyu
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private final UserService userService;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;
    private final ChatHistoryService chatHistoryService;

    public AppServiceImpl(UserService userService, AiCodeGeneratorFacade aiCodeGeneratorFacade,
                          ChatHistoryService chatHistoryService) {
        this.userService = userService;
        this.aiCodeGeneratorFacade = aiCodeGeneratorFacade;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * Convert an App entity to an AppVO.
     *
     * @param app the entity
     * @return AppVO
     */
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

    /**
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
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

        chatHistoryService.addChatMessage(appId, message, ChatHistoryTypeEnum.USER.getValue(), loginUser.getId());

        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenType, appId);

        StringBuilder aiResponseBuilder = new StringBuilder();
        return contentFlux
                .map(chunk -> {
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryTypeEnum.AI.getValue(), loginUser.getId());
                    }
                })
                .doOnError(throwable -> {
                    String errorMsg = "AI response error: " + throwable.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMsg, ChatHistoryTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    @Override
    // @Transactional(rollbackFor = Exception.class)
    // It's acceptable that the app is deleted but chat message remains
    public boolean removeById(Serializable id) {
        if  (id == null) return false;
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) return false;

        try {
            chatHistoryService.removeByAppId(appId);
        } catch (Exception e) {
            log.error("remove chat message error: {}", e.getMessage());
        }

        return super.removeById(id);
    }

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

        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }
}
