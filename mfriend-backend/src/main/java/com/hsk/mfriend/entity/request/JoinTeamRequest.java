package com.hsk.mfriend.entity.request;

import lombok.Data;

/**
 * Date:2024/06/25
 * Author:hsk
 */
@Data
public class JoinTeamRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;

}
