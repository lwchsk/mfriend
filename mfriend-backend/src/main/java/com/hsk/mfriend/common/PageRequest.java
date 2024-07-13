package com.hsk.mfriend.common;

import lombok.Data;

/**
 * 分页通用参数
 * Date:2024/06/24
 * Author:hsk
 */
@Data
public class PageRequest {
    /**
     * 页面大小
     */
    protected int pageSize;
    /**
     * 当前是第几页
     */
    protected int pageNum;
}
