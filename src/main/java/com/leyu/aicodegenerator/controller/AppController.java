package com.leyu.aicodegenerator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leyu.aicodegenerator.annotation.AuthCheck;
import com.leyu.aicodegenerator.common.BaseResponse;
import com.leyu.aicodegenerator.common.DeleteRequest;
import com.leyu.aicodegenerator.common.ResultUtils;
import com.leyu.aicodegenerator.constant.AppConstant;
import com.leyu.aicodegenerator.constant.UserConstant;
import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.exception.ThrowUtils;
import com.leyu.aicodegenerator.model.dto.app.*;
import com.leyu.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.leyu.aicodegenerator.model.vo.app.AppVO;
import com.leyu.aicodegenerator.ratelimiter.annotation.RateLimit;
import com.leyu.aicodegenerator.ratelimiter.enums.RateLimitType;
import com.leyu.aicodegenerator.service.AppService;
import com.leyu.aicodegenerator.service.ProjectDownloadService;
import com.leyu.aicodegenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App Controller
 *
 * @author Lyu
 */
/** AppController implementation. */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;
    @Resource
    private CacheManager cacheManager;

    // ==================== User Endpoints ====================

    /**
     * Create a new app.
     * initPrompt is required.
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);

    }

    /**
     * Update the current user's own app (app name only).
     * Returns NO_AUTH_ERROR if the app does not belong to the current user.
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        App existingApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(existingApp.getUserId() == null || !existingApp.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);

        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());

        // set edit time
        app.setEditTime(LocalDateTime.now());

        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Delete the current user's own app.
     * Returns NO_AUTH_ERROR if the app does not belong to the current user.
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        App existingApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(existingApp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(existingApp.getUserId() == null ||
                (!existingApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())),
                ErrorCode.NO_AUTH_ERROR);

        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Get app detail by id. Any logged-in user may view any app.
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        userService.getLoginUser(request);

        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * Paginated list of the current user's own apps.
     * Supports fuzzy search by app name. Page size is capped at 20.
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        int pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "20 apps at most for each page");
        long pageNum = appQueryRequest.getPageNum();

        appQueryRequest.setUserId(loginUser.getId());

        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);

        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * Paginated list of featured apps (priority > 0), ordered by priority descending.
     * Supports fuzzy search by app name. Page size is capped at 20.
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.leyu.aicodegenerator.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"

    )
    /** listFeaturedAppVOByPage implementation. */
    public BaseResponse<Page<AppVO>> listFeaturedAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        int pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "20 apps at most for each page");
        long pageNum = appQueryRequest.getPageNum();

        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        queryWrapper
                .isNotNull("deployedTime")
                .isNotNull("deployKey")
                .ne("deployKey", "");

        Page<App> appPage = appService.page(
                Page.of(pageNum, pageSize),
                queryWrapper
        );

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    // ==================== Admin Endpoints ====================

    /**
     * Admin: delete any app by id.
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(deleteRequest.getId());

        return ResultUtils.success(result);
    }

    /**
     * Admin: update any app (name, cover, priority).
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);

        Long id = appAdminUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        Integer newPriority = appAdminUpdateRequest.getPriority();
        if (newPriority != null && AppConstant.GOOD_APP_PRIORITY.equals(newPriority)) {
            boolean deployed = StrUtil.isNotBlank(oldApp.getDeployKey()) && oldApp.getDeployedTime() != null;
            ThrowUtils.throwIf(!deployed, ErrorCode.OPERATION_ERROR, "Only deployed apps can be featured");
        }

        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        boolean needClearGoodAppCache = newPriority != null
                && (AppConstant.GOOD_APP_PRIORITY.equals(newPriority)
                || AppConstant.GOOD_APP_PRIORITY.equals(oldApp.getPriority()));
        if (needClearGoodAppCache) {
            Cache goodAppCache = cacheManager.getCache("good_app_page");
            if (goodAppCache != null) {
                goodAppCache.clear();
            }
        }
        return ResultUtils.success(true);
    }

    /**
     * Admin: paginated list of all apps with any non-time field as filter.
     * No page size limit is enforced.
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> adminListAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();

        Page<App> appPage = appService.page(
                Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest)
        );

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * Admin: get app detail by id (returns full entity including sensitive fields).
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> adminGetAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI chat requests are too frequent. Please try again later.")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                               @RequestParam String message,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid app id");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "User message cannot be null");

        User loginUser = userService.getLoginUser(request);

        Flux<String> stringFlux = appService.chatToGenCode(appId, message, loginUser);

        return stringFlux.map(chunk -> {
            Map<String, String> wrapper = Map.of("d", chunk);
            String jsonData = JSONUtil.toJsonStr(wrapper);
            return ServerSentEvent.<String>builder()
                    .data(jsonData)
                    .build();
        })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /** deployApp implementation. */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR,  "App id cannot be null");

        User loginUser = userService.getLoginUser(request);

        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App id is invalid");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "App not found");

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "No permission to download the code");

        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "Application code not exist, please generate code first");
        String downloadFileName = String.valueOf(appId);
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }
}
