package com.somepro.domain.pawner.model;

/**
 * 当户名单查询条件（不可变值对象）。
 *
 * - name / idCard / phone 做模糊匹配，任一项为 null/空串即不参与过滤；
 * - status 为 null 表示「不按状态筛」——此时默认名册不含已注销档案
 *   （注销的人不应再在名单里看到，但账留着；显式传 CLOSED 仍可把注销档案调出来）；
 * - status 非 null 时必须是三个合法状态之一，非法写法在解析阶段就被挡回。
 */
public record PawnerQuery(String name, String idCard, String phone, PawnerStatus status) {

    /** 空条件：一个都不填，翻整份名册（不含已注销）。 */
    public static PawnerQuery of(String name, String idCard, String phone, PawnerStatus status) {
        return new PawnerQuery(blankToNull(name), blankToNull(idCard), blankToNull(phone), status);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
