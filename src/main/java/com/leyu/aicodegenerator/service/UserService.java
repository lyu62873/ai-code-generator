package com.leyu.aicodegenerator.service;

import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.dto.user.UserQueryRequest;
import com.leyu.aicodegenerator.model.vo.user.LoginUserVO;
import com.leyu.aicodegenerator.model.vo.user.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.leyu.aicodegenerator.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 *  User Service
 *
 * @author Lyu
 */
public interface UserService extends IService<User> {

    /**
     * User Register
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return user id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * Get desensitized info about login user
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * User Login
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * Get current login user
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * User Logout
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * get User VO
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * get User VO List
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);
}
