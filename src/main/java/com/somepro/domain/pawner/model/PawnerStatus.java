package com.somepro.domain.pawner.model;

import com.somepro.common.exception.BizException;

/**
 * 当户状态（领域枚举，纯业务、无框架注解）。
 *
 * 只认这三个值：
 * - NORMAL 正常（新建档案默认值）
 * - FROZEN 冻结（临时停办业务，档案仍在名册里）
 * - CLOSED 注销（业务终结，档案留账但名册不可见；注销只能走注销用例，不允许随手改出来）
 *
 * 外部传入任何别的写法一律不收（{@link #parse} 直接抛业务异常）。
 */
public enum PawnerStatus {

    NORMAL,
    FROZEN,
    CLOSED;

    /** 按字符串解析状态；null 或非约定写法都拒绝，避免脏值落库。 */
    public static PawnerStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("状态不能为空，只支持 NORMAL / FROZEN / CLOSED");
        }
        String normalized = value.trim();
        for (PawnerStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new BizException("非法状态值：" + value + "，只支持 NORMAL / FROZEN / CLOSED");
    }

    /** 注销档案：业务上仍在，但从名册查询里隐去。 */
    public boolean isClosed() {
        return this == CLOSED;
    }
}
