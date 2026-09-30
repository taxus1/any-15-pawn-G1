package com.somepro.domain.pawner.model;

/**
 * 名册查询条件（领域值对象，不可变）。
 *
 * 姓名/身份证/电话/状态任意组合都可查；全部为 null 时调出整份名册。
 * 注意名册天然不含已注销（CLOSED）档案 —— 注销是「留账除名」，由仓储实现统一追加该条件。
 *
 * - name/phone：模糊匹配（柜台按片段找）；
 * - idCard：精确匹配（身份证是认人凭证，片段匹配没有业务意义）；
 * - status：非空时精确过滤，传 CLOSED 不允许（注销档案不在名册，查了也是空/拒绝，接口层提前拒绝非法值）。
 */
public record PawnerCondition(String name, String idCard, String phone, PawnerStatus status) {

    public static PawnerCondition of(String name, String idCard, String phone, PawnerStatus status) {
        return new PawnerCondition(blankToNull(name), blankToNull(idCard), blankToNull(phone), status);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
