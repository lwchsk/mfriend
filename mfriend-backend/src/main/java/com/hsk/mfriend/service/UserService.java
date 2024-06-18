package com.hsk.mfriend.service;

import com.hsk.mfriend.entity.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author hsk
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-05-02
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 用户id
     */
    long uerRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return User
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    User getSafetyUser(User user);
    /**
     * 用户注销
     */
    int userLogout(HttpServletRequest request);

    List<User> searchUsersByTags(List<String> tagNameList);
}
