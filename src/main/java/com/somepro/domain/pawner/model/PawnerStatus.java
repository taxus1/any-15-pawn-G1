package com.somepro.domain.pawner.model;

import com.somepro.common.exception.BizException;

/**
 * 当户状态（纯领域枚举，不依赖任何框架）。
 *
 * 只有这三个值合法，接口层传入别的写法一律不收（{@link #ofCode(String)} 抛业务异常）。
 * <ul>
 *   <li>{@link #NORMAL} 正常</li>
 *   <li>{@link #FROZEN} 冻结</li>
 *   <li>{@link #CLOSED} 注销</li>
 * </ul>
 * 用 code 落库（status 列存枚举名），而不是 ordinal：列内容可读，且枚举顺序调整不会污染历史数据。
 */
public enum PawnerStatus {

    NORMAL("NORMAL", "正常"),
    FROZEN("FROZEN", "冻结"),
    CLOSED("CLOSED", "注销");

    private final String code;
    private final String label;

    PawnerStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 由外部传入的状态值解析枚举：只认 NORMAL / FROZEN / CLOSED（大小写敏感，列里就是这么存的），
     * 传 null、空串或其它写法都算非法入参。
     */
    public static PawnerStatus ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (PawnerStatus status : values()) {
            if (status.code.equals(trimmed)) {
                return status;
            }
        }
        throw new BizException("状态只支持 NORMAL / FROZEN / CLOSED：" + code);
    }

    /** 未注销：注销后的历史档案不参与「同一张身份证只能有一份」的约束。 */
    public boolean isActive() {
        return this != CLOSED;
    }
}
