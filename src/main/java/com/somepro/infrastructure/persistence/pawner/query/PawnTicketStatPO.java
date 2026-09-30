package com.somepro.infrastructure.persistence.pawner.query;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_pawn_ticket（当票）的只读投影 PO（基础设施层）。
 *
 * 当票模块本身尚未落地，当户模块的注销校验 / 详情对账需要从这张表实时计数，
 * 这里只映射计数用得到的列（id / pawner_id / status），不做任何写入。
 * del_flag 由 @TableLogic 自动过滤，计数只算未删除行。
 */
@Getter
@Setter
@TableName("t_pawn_ticket")
public class PawnTicketStatPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("pawner_id")
    private Long pawnerId;

    /** ACTIVE 在当 / REDEEMED 已赎 / FORFEITED 已绝当 / CANCELLED 已撤销 */
    @TableField("status")
    private String status;
}
