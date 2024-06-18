package com.hsk.mfriend.service.impl;

import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Date:2024/06/14
 * Author:hsk
 */
@SpringBootTest
class UserServiceImplTest {
    @Resource
    UserService userService;
    @Test
    void searchUsersByTags() {
        List<String> tagNames = Arrays.asList("java","python");
        List<User> users = userService.searchUsersByTags(tagNames);
        Assertions.assertTrue(users.size()==1);
    }
}