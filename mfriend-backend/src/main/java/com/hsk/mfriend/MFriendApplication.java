package com.hsk.mfriend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hsk.mfriend.mapper")
public class MFriendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MFriendApplication.class, args);
    }

}
