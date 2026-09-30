package com.somepro.domain.pawner.model;

/**
 * 当户名下联当业务的对账数据（领域值对象，不可变）。
 *
 * 数字直接从当物、当票两张表实时统计，必须与那两个模块的记录对得上：
 * - {@code pledgedCount} 名下还押在行里的当物数（未了结）：t_collateral.status IN (IN_STOCK, PAWNED)
 * - {@code inStockCount} 其中还躺在库里的件数：t_collateral.status = IN_STOCK
 * - {@code activeTicketCount} 名下没走完的当票数：t_pawn_ticket.status = ACTIVE
 *
 * 注销前置校验就看 pledgedCount 与 activeTicketCount 是否都为 0。
 * （IN_STOCK 已登记未开票、PAWNED 已开票在当，都算「还压在行里」；
 *   REDEEMED 已赎回 / FORFEITED 已绝当 / RELEASED 已退还为终态，不算。）
 */
public record PawnerAssets(long pledgedCount, long inStockCount, long activeTicketCount) {

    /** 名下是否已结清：没有未了结当物、也没有在当当票，才可注销。 */
    public boolean allSettled() {
        return pledgedCount == 0 && activeTicketCount == 0;
    }
}
