package com.hsk.mfriend.service.impl;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hsk.mfriend.common.ErrorCode;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.exception.BusinessException;
import com.hsk.mfriend.mapper.UserMapper;
import com.hsk.mfriend.service.UserService;
import com.hsk.mfriend.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hsk.mfriend.constant.UserConstant.ADMIN_ROLE;
import static com.hsk.mfriend.constant.UserConstant.USER_LOGIN_STATE;


/**
 * @author dachui_boom
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-05-02 10:34:12
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final String SALT = "hsk";
    @Resource
    private UserMapper userMapper;

    @Override
    public long uerRegister(String userAccount, String userPassword, String checkPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return -1;
        }
        if (userAccount.length() < 4)
            return -1;
        if (userPassword.length() < 8 || checkPassword.length() < 8)
            return -1;

        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }

        if (!checkPassword.equals(userPassword))
            return -1;

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0)
            return -1;

        String encryPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());


        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();

    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户和密码为空");
        }
        if (userAccount.length() < 4)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户小于四位");
        if (userPassword.length() < 8)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于八位");

        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户不能有特殊字符");
        }


        String encryPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户或密码错误");
        }

        User safetyUser = getSafetyUser(user);
        //4.向服务器记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    @Override
    public User getSafetyUser(User user) {
        //3.脱敏
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setUserRole(user.getUserRole());
        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户，内存版
     *
     * @param tagNameList
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            //TODO 可以尝试orelse（）方法减少if分支
            if (tagsStr == null) {
                return false;
            }
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

    }

    @Override
    public Integer updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return userMapper.updateById(user);
    }

    /**
     * 根据标签搜索用户，SQL版
     *
     * @param tagNameList
     * @return
     */
    @Deprecated
    private List<User> searchUsersBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 拼接 AND 查询：like '%Java%' AND like '%Python%'
        for (String tagName : tagNameList) {
            queryWrapper.like("tags", tagName);
        }

        return userMapper.selectList(queryWrapper).stream().map(this::getSafetyUser).collect(Collectors.toList());

    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return userObj != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }


}


//
//package com.yupi.yupao.service.impl;
//
//        import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//        import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//        import com.google.gson.Gson;
//        import com.google.gson.TypeAdapter;
//        import com.google.gson.reflect.TypeToken;
//        import com.yupi.yupao.common.ErrorCode;
//        import com.yupi.yupao.constant.UserConstant;
//        import com.yupi.yupao.exception.BusinessException;
//        import com.yupi.yupao.model.domain.User;
//        import com.yupi.yupao.model.vo.UserVO;
//        import com.yupi.yupao.service.UserService;
//        import com.yupi.yupao.mapper.UserMapper;
//        import com.yupi.yupao.utils.AlgorithmUtils;
//        import lombok.extern.slf4j.Slf4j;
//        import org.apache.commons.lang3.StringUtils;
//        import org.apache.commons.math3.util.Pair;
//        import org.springframework.stereotype.Service;
//        import org.springframework.util.CollectionUtils;
//        import org.springframework.util.DigestUtils;
//
//        import javax.annotation.Resource;
//        import javax.servlet.http.HttpServletRequest;
//        import java.util.*;
//        import java.util.regex.Matcher;
//        import java.util.regex.Pattern;
//        import java.util.stream.Collectors;
//        import java.util.stream.Stream;
//
//        import static com.yupi.yupao.constant.UserConstant.USER_LOGIN_STATE;
//
///**
// * 用户服务实现类
// *
// * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
// * @from <a href="https://yupi.icu">编程导航知识星球</a>
// */
//@Service
//@Slf4j
//public class UserServiceImpl extends ServiceImpl<UserMapper, User>
//        implements UserService {
//
//    @Resource
//    private UserMapper userMapper;
//
//    /**
//     * 盐值，混淆密码
//     */
//    private static final String SALT = "yupi";
//
//    @Override
//    public long userRegister(String userAccount, String userPassword, String checkPassword, String userId) {
//        // 1. 校验
//        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, userId)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
//        }
//        if (userAccount.length() < 4) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
//        }
//        if (userPassword.length() < 8 || checkPassword.length() < 8) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
//        }
//        if (userId.length() > 5) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
//        }
//        // 账户不能包含特殊字符
//        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
//        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
//        if (matcher.find()) {
//            return -1;
//        }
//        // 密码和校验密码相同
//        if (!userPassword.equals(checkPassword)) {
//            return -1;
//        }
//        // 账户不能重复
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("userAccount", userAccount);
//        long count = userMapper.selectCount(queryWrapper);
//        if (count > 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
//        }
//        // 星球编号不能重复
//        queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("userId", userId);
//        count = userMapper.selectCount(queryWrapper);
//        if (count > 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
//        }
//        // 2. 加密
//        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
//        // 3. 插入数据
//        User user = new User();
//        user.setUserAccount(userAccount);
//        user.setUserPassword(encryptPassword);
//        user.setuserId(userId);
//        boolean saveResult = this.save(user);
//        if (!saveResult) {
//            return -1;
//        }
//        return user.getId();
//    }
//
//    // [加入编程导航](https://www.code-nav.cn/) 入门捷径+交流答疑+项目实战+求职指导，帮你自学编程不走弯路
//
//    @Override
//    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
//        // 1. 校验
//        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
//            return null;
//        }
//        if (userAccount.length() < 4) {
//            return null;
//        }
//        if (userPassword.length() < 8) {
//            return null;
//        }
//        // 账户不能包含特殊字符
//        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
//        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
//        if (matcher.find()) {
//            return null;
//        }
//        // 2. 加密
//        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
//        // 查询用户是否存在
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("userAccount", userAccount);
//        queryWrapper.eq("userPassword", encryptPassword);
//        User user = userMapper.selectOne(queryWrapper);
//        // 用户不存在
//        if (user == null) {
//            log.info("user login failed, userAccount cannot match userPassword");
//            return null;
//        }
//        // 3. 用户脱敏
//        User safetyUser = getSafetyUser(user);
//        // 4. 记录用户的登录态
//        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
//        return safetyUser;
//    }
//
//    /**
//     * 用户脱敏
//     *
//     * @param originUser
//     * @return
//     */
//    @Override
//    public User getSafetyUser(User originUser) {
//        if (originUser == null) {
//            return null;
//        }
//        User safetyUser = new User();
//        safetyUser.setId(originUser.getId());
//        safetyUser.setUsername(originUser.getUsername());
//        safetyUser.setUserAccount(originUser.getUserAccount());
//        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
//        safetyUser.setGender(originUser.getGender());
//        safetyUser.setPhone(originUser.getPhone());
//        safetyUser.setEmail(originUser.getEmail());
//        safetyUser.setuserId(originUser.getuserId());
//        safetyUser.setUserRole(originUser.getUserRole());
//        safetyUser.setUserStatus(originUser.getUserStatus());
//        safetyUser.setCreateTime(originUser.getCreateTime());
//        safetyUser.setTags(originUser.getTags());
//        return safetyUser;
//    }
//
//    /**
//     * 用户注销
//     *
//     * @param request
//     */
//    @Override
//    public int userLogout(HttpServletRequest request) {
//        // 移除登录态
//        request.getSession().removeAttribute(USER_LOGIN_STATE);
//        return 1;
//    }
//
//    /**
//     * 根据标签搜索用户（内存过滤）
//     *
//     * @param tagNameList 用户要拥有的标签
//     * @return
//     */
//    @Override
//    public List<User> searchUsersByTags(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // 1. 先查询所有用户
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        List<User> userList = userMapper.selectList(queryWrapper);
//        Gson gson = new Gson();
//        // 2. 在内存中判断是否包含要求的标签
//        return userList.stream().filter(user -> {
//            String tagsStr = user.getTags();
//            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
//            }.getType());
//            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
//            for (String tagName : tagNameList) {
//                if (!tempTagNameSet.contains(tagName)) {
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
//    }
//
//    @Override
//    public int updateUser(User user, User loginUser) {
//        long userId = user.getId();
//        if (userId <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // todo 补充校验，如果用户没有传任何要更新的值，就直接报错，不用执行 update 语句
//        // 如果是管理员，允许更新任意用户
//        // 如果不是管理员，只允许更新当前（自己的）信息
//        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
//            throw new BusinessException(ErrorCode.NO_AUTH);
//        }
//        User oldUser = userMapper.selectById(userId);
//        if (oldUser == null) {
//            throw new BusinessException(ErrorCode.NULL_ERROR);
//        }
//        return userMapper.updateById(user);
//    }
//
//
//    /**
//     * 是否为管理员
//     *
//     * @param request
//     * @return
//     */
//    @Override
//    public boolean isAdmin(HttpServletRequest request) {
//        // 仅管理员可查询
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User user = (User) userObj;
//        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
//    }
//
//    /**
//     * 是否为管理员
//     *
//     * @param loginUser
//     * @return
//     */
//    @Override
//    public boolean isAdmin(User loginUser) {
//        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
//    }
//
//    @Override
//    public List<User> matchUsers(long num, User loginUser) {
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.select("id", "tags");
//        queryWrapper.isNotNull("tags");
//        List<User> userList = this.list(queryWrapper);
//        String tags = loginUser.getTags();
//        Gson gson = new Gson();
//        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
//        }.getType());
//        // 用户列表的下标 => 相似度
//        List<Pair<User, Long>> list = new ArrayList<>();
//        // 依次计算所有用户和当前用户的相似度
//        for (int i = 0; i < userList.size(); i++) {
//            User user = userList.get(i);
//            String userTags = user.getTags();
//            // 无标签或者为当前用户自己
//            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
//                continue;
//            }
//            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
//            }.getType());
//            // 计算分数
//            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
//            list.add(new Pair<>(user, distance));
//        }
//        // 按编辑距离由小到大排序
//        List<Pair<User, Long>> topUserPairList = list.stream()
//                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
//                .limit(num)
//                .collect(Collectors.toList());
//        // 原本顺序的 userId 列表
//        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
//        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
//        userQueryWrapper.in("id", userIdList);
//        // 1, 3, 2
//        // User1、User2、User3
//        // 1 => User1, 2 => User2, 3 => User3
//        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
//                .stream()
//                .map(user -> getSafetyUser(user))
//                .collect(Collectors.groupingBy(User::getId));
//        List<User> finalUserList = new ArrayList<>();
//        for (Long userId : userIdList) {
//            finalUserList.add(userIdUserListMap.get(userId).get(0));
//        }
//        return finalUserList;
//    }
//
//    /**
//     * 根据标签搜索用户（SQL 查询版）
//     *
//     * @param tagNameList 用户要拥有的标签
//     * @return
//     */
//    @Deprecated
//    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        // 拼接 and 查询
//        // like '%Java%' and like '%Python%'
//        for (String tagName : tagNameList) {
//            queryWrapper = queryWrapper.like("tags", tagName);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
//    }
//
//}
//
//
//
//
