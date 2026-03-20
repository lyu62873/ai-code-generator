package com.leyu.aicodegenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import com.leyu.aicodegenerator.model.dto.user.UserQueryRequest;
import com.leyu.aicodegenerator.model.enums.UserRoleEnum;
import com.leyu.aicodegenerator.model.vo.user.LoginUserVO;
import com.leyu.aicodegenerator.model.vo.user.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.leyu.aicodegenerator.entity.User;
import com.leyu.aicodegenerator.mapper.UserMapper;
import com.leyu.aicodegenerator.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

import static com.leyu.aicodegenerator.constant.UserConstant.USER_LOGIN_STATE;

/**
 *  User Service Impl
 *
 * @author Lyu
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    /**
     * User Register
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return user id
     */
/** User Register. */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters unfilled");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account too short");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Password unmatch");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Password too short");
        }

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount)
                .eq("isDelete", 0);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account already exist");
        }

        String encryptPassword = getEncryptPassword(userPassword);

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("Unnamed Vibe Coder");
        user.setUserRole(UserRoleEnum.USER.getValue());

        boolean saveRes = this.save(user);
        if (!saveRes) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Register fail because of database error");
        }

        return user.getId();
    }

    /**
     * Get info about login user
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) return null;

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * User Login
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
/** User Login. */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters unfilled");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account incorrect");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Password incorrect");
        }

        String encryptPassword = getEncryptPassword(userPassword);

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount)
                .eq("userPassword", encryptPassword)
                .eq("isDelete", 0);

        User one = this.getOne(queryWrapper);

        if (ObjUtil.isEmpty(one)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Login fail, Account not exist or Password incorrect");
        }

        request.getSession().setAttribute(USER_LOGIN_STATE, one);

        return getLoginUserVO(one);
    }

    /**
     * get current login user
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) attribute;

        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        long userId = currentUser.getId();
        currentUser = getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return currentUser;
    }

    /**
     * User Logout
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "not login");
        }

        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * get User VO
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * get User VO List
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) return new ArrayList<>();

        return userList.stream().map(this::getUserVO).toList();
    }

    /**
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters unfilled");
        }

        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * Password Encryption
     * @param userPassword
     * @return EncryptPassword
     */
    public String getEncryptPassword(String userPassword) {
        final String SALT = "karma";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
