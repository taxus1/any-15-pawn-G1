package com.somepro.domain.pawner.model;

/**
 * 当户对账快照（不可变值对象）：详情页展示的三个数，必须与当物、当票两张表里的记录对得上。
 *
 * 计数口径（均只计 del_flag=0 的有效记录）：
 * - heldItemCount 名下还押在行里的当物：t_collateral.status 为 IN_STOCK（在库）或 PAWNED（已典当），
 *   即还没走到 REDEEMED / FORFEITED / RELEASED 终态的；
 * - inStockCount 还躺在库里的当物：t_collateral.status = IN_STOCK；
 * - activeTicketCount 没走完的当票：t_pawn_ticket.status = ACTIVE（在当）。
 *
 * heldItemCount 恒 ≥ inStockCount；注销前置校验看 heldItemCount 与 activeTicketCount 是否都为 0。
 */
public record PawnerLedger(long heldItemCount, long inStockCount, long activeTicketCount) {

    public boolean clean() {
        return heldItemCount == 0 && activeTicketCount == 0;
    }
}
