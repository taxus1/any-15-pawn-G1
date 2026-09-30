package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 与领域层 {@code PageResult} 分工：PageResult 不带框架注解、只有 content/total/pageNum/pageSize；
 * 派生的 totalPages 放接口层（record 只序列化组件，在领域 record 上加 @JsonProperty 会把 Jackson 引进领域层）。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
