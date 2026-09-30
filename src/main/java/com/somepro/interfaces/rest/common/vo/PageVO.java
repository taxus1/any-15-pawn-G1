package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（接口层共享，不可变 record）。
 * 比领域层 {@code PageResult} 多一个 totalPages，避免把 Jackson 注解带进领域层（与 demo 模块同款取舍）。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
