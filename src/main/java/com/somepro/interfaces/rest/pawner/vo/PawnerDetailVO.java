package com.somepro.interfaces.rest.pawner.vo;

import java.io.Serializable;

/**
 * 当户详情对外对象（不可变 record）：档案字段 + 对账三个数。
 *
 * - heldItemCount 名下还押在行里的当物件数（在库 + 已典当）
 * - inStockCount 名下还躺在库里的当物件数
 * - activeTicketCount 名下在当（没走完）的当票笔数
 * 三个数实时来自当物、当票两张表，与那边的记录一致。
 */
public record PawnerDetailVO(PawnerVO pawner,
                             long heldItemCount,
                             long inStockCount,
                             long activeTicketCount) implements Serializable {
}
