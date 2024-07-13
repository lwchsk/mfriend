package com.hsk.mfriend.entity.request;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * Date:2024/05/03
 * Author:hsk
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -3838865739577030580L;

    private String userAccount;
    private String userPassword;
    private HttpServletRequest request;
}
