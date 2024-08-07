package com.hsk.mfriend.entity.request;

import lombok.Data;

import java.io.Serializable;

/**
 * Date:2024/05/03
 * Author:hsk
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -8784552673831748716L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
