package com.hsk.mfriend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hsk.mfriend.common.BaseResponse;
import com.hsk.mfriend.common.ErrorCode;
import com.hsk.mfriend.common.ResultUtils;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.entity.domain.request.UserLoginRequest;
import com.hsk.mfriend.entity.domain.request.UserRegisterRequest;
import com.hsk.mfriend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.hsk.mfriend.constant.UserConstant.DEFAULT_ROLE;
import static com.hsk.mfriend.constant.UserConstant.USER_LOGIN_STATE;


/**
 * Date:2024/05/03
 * Author:hsk
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_NULL);
        }

        if (StringUtils.isAnyBlank(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword(),
                request);
        return ResultUtils.success(user);
    }

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_NULL);
        }
        //用变量接受从request得到的数据，然后传递给方法比较规范
        if (StringUtils.isAnyBlank(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword())) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        Long result = userService.uerRegister(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword());
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> userDelete(@RequestBody long id, HttpServletRequest request) {
        if (!isUser(request)) {
            return ResultUtils.error(ErrorCode.NO_AUTH);
        }
        Boolean result = userService.removeById(id);
        return ResultUtils.success(result);

    }

    @GetMapping("/search")
    public BaseResponse<List<User>> userSearch(String username, HttpServletRequest request) {
        if (isUser(request)) {
            return ResultUtils.error(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        //放进来防止为空报错
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> list = userService.list(queryWrapper);
        List<User> result = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(result);

    }


    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null) {
            return ResultUtils.error(ErrorCode.NO_LOGIN);
        }
        Long id = user.getId();
        User currentUser = userService.getById(id);
        User result = userService.getSafetyUser(currentUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> logout(HttpServletRequest request) {
        if (request == null) {
            return ResultUtils.error(ErrorCode.PARAMS_NULL);
        }
        Integer result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    public boolean isUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return userObj == null || user.getUserRole() == DEFAULT_ROLE;
    }


}
