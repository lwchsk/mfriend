package com.hsk.mfriend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hsk.mfriend.common.BaseResponse;
import com.hsk.mfriend.common.ErrorCode;
import com.hsk.mfriend.common.ResultUtils;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.entity.request.UserLoginRequest;
import com.hsk.mfriend.entity.request.UserRegisterRequest;
import com.hsk.mfriend.exception.BusinessException;
import com.hsk.mfriend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

import static com.hsk.mfriend.constant.UserConstant.*;


/**
 * Date:2024/05/03
 * Author:hsk
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = {"*"})
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

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
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


    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User loginUser = (User) userObj;
        return ResultUtils.success(userService.updateUser(user, loginUser));
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> getRecommendUser(long pageSize, long pageNum) {
//        User loginUser = userService.getLoginUser(request);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userList = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(userList);
    }

    /**
     * 获取最匹配的用户数据
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> getMatchUser(Long num, HttpServletRequest request) {

//         设计：通过num参数控制匹配显示的数据数

        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数量超出范围");
        }
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userService.matchUsers(num, loginUser);
        return ResultUtils.success(userList);
    }

}
///**
// * 用户接口
// */
//@RestController
//@RequestMapping("/user")
//@CrossOrigin(origins = {"http://localhost:3000"})
//@Slf4j
//public class UserController {
//
//    @Resource
//    private UserService userService;
//
//    @Resource
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @PostMapping("/register")
//    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
//        if (userRegisterRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        String userAccount = userRegisterRequest.getUserAccount();
//        String userPassword = userRegisterRequest.getUserPassword();
//        String checkPassword = userRegisterRequest.getCheckPassword();
//        String userId = userRegisterRequest.getuserId();
//        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, userId)) {
//            return null;
//        }
//        long result = userService.userRegister(userAccount, userPassword, checkPassword, userId);
//        return ResultUtils.success(result);
//    }
//
//    @PostMapping("/login")
//    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
//        if (userLoginRequest == null) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
//        }
//        String userAccount = userLoginRequest.getUserAccount();
//        String userPassword = userLoginRequest.getUserPassword();
//        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
//        }
//        User user = userService.userLogin(userAccount, userPassword, request);
//        return ResultUtils.success(user);
//    }
//
//    @PostMapping("/logout")
//    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
//        if (request == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        int result = userService.userLogout(request);
//        return ResultUtils.success(result);
//    }
//
//    @GetMapping("/current")
//    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User currentUser = (User) userObj;
//        if (currentUser == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN);
//        }
//        long userId = currentUser.getId();
//        // TODO 校验用户是否合法
//        User user = userService.getById(userId);
//        User safetyUser = userService.getSafetyUser(user);
//        return ResultUtils.success(safetyUser);
//    }
//
//    @GetMapping("/search")
//    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
//        if (!userService.isAdmin(request)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        if (StringUtils.isNotBlank(username)) {
//            queryWrapper.like("username", username);
//        }
//        List<User> userList = userService.list(queryWrapper);
//        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
//        return ResultUtils.success(list);
//    }
//
//    @GetMapping("/search/tags")
//    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        List<User> userList = userService.searchUsersByTags(tagNameList);
//        return ResultUtils.success(userList);
//    }
//
//    // todo 推荐多个，未实现
//    @GetMapping("/recommend")
//    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
//        User loginUser = userService.getLoginUser(request);
//        String redisKey = String.format("yupao:user:recommend:%s", loginUser.getId());
//        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
//        // 如果有缓存，直接读缓存
//        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
//        if (userPage != null) {
//            return ResultUtils.success(userPage);
//        }
//        // 无缓存，查数据库
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
//        // 写缓存
//        try {
//            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
//        } catch (Exception e) {
//            log.error("redis set key error", e);
//        }
//        return ResultUtils.success(userPage);
//    }
//
//
//    @PostMapping("/update")
//    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
//        // 校验参数是否为空
//        if (user == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        int result = userService.updateUser(user, loginUser);
//        return ResultUtils.success(result);
//    }
//
//    @PostMapping("/delete")
//    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
//        if (!userService.isAdmin(request)) {
//            throw new BusinessException(ErrorCode.NO_AUTH);
//        }
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean b = userService.removeById(id);
//        return ResultUtils.success(b);
//    }
//
//    /**
//     * 获取最匹配的用户
//     *
//     * @param num
//     * @param request
//     * @return
//     */
//    @GetMapping("/match")
//    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
//        if (num <= 0 || num > 20) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = userService.getLoginUser(request);
//        return ResultUtils.success(userService.matchUsers(num, user));
//    }
//
//}
