package com.leyu.aicodegenerator.service;

import com.leyu.aicodegenerator.entity.App;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.model.dto.app.AppQueryRequest;
import com.leyu.aicodegenerator.model.vo.app.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * App Service
 *
 * @author Lyu
 */
public interface AppService extends IService<App> {

    /**
     * Convert an App entity to an AppVO.
     *
     * @param app the entity
     * @return AppVO
     */
    AppVO getAppVO(App app);

    /**
     * Convert a list of App entities to a list of AppVOs.
     *
     * @param appList the entity list
     * @return AppVO list
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * Build a QueryWrapper for user-facing app queries (by app name).
     * Caller is responsible for adding the userId filter when querying "my apps".
     *
     * @param appQueryRequest the query request
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     *
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);
}
