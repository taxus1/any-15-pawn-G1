package com.somepro.interfaces.rest.pawner.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当户详情（接口层出参）：档案字段平铺，外加与当物、当票两表实时对账的三个数。
 *
 * - pledgedCount 名下还押在行里的当物数（在库 + 已典当，未了结）
 * - inStockCount 其中还躺在库里的件数
 * - activeTicketCount 名下没走完的当票数
 *
 * 已注销档案也查得到详情（留账），但名册里不再出现。
 */
public record PawnerDetailVO(
        Long id,
        String pawnerNo,
        String name,
        String idCard,
        String phone,
        String address,
        String status,
        LocalDateTime createTime,
        long pledgedCount,
        long inStockCount,
        long activeTicketCount
) implements Serializable {
}
